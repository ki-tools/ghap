package io.ghap.userdata.contribution;



import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.userdata.contribution.storage.FileInfo;
import io.ghap.userdata.contribution.storage.FileStream;
import io.ghap.userdata.contribution.storage.ObjectListing;
import io.ghap.userdata.contribution.storage.s3.Utils;
import io.ghap.userdata.logevents.EventLogger;
import io.ghap.userdata.logevents.FileEvent;
import io.ghap.userdata.logevents.ProjectProvisioningClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Collections.EMPTY_LIST;

@Singleton
@Path("UserData")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "BMGF Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class UserDataResource
{
  private static final String BEARER_PREFIX = "Bearer ";
  private static String INVALID_CHARS = "^[^<>:\"/\\\\|?*]+$";
  private static Pattern NAME_PATTERN = Pattern.compile(INVALID_CHARS);
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  private Storage storage;

  @Inject
  private EventLogger eventLogger;

  @Inject
  private ProjectProvisioningClient projectProvisioningClient;

  @Context
  private SecurityContext securityContext;

  @Configuration("userdata.name")
  private String userDataName;

  /**
   * Contribute a file to the DataSubmission S3 bucket all files
   * are encrypted server side when stored.
   *
   * @param uuid The Unique User Identifier this is used to locate the users storage location in S3
   * @param inputStream The stream to write to the S3 Bucket
   * @param contentDisposition Content Disposition of the inputStream
   * @return <li>
   * <ul>{@link Status#OK} on success
   * <ul>{@link Status#NOT_FOUND} if the bucket doesn't exist</ul>
   * <ul>{@link Status#CONFLICT} if the key already exists
   * <ul>{@link Status#NO_CONTENT} on error</ul>
   * </li>
   */
  @Path("/submit/{uuid}")
  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response submit(@PathParam("uuid") String uuid,
                         @FormDataParam("file") InputStream inputStream,
                         @FormDataParam("file") FormDataContentDisposition contentDisposition)
  {
    return create(getUserName(), contentDisposition.getFileName(), inputStream);
  }

  /**
   * Contribute a file to the DataSubmission S3 bucket all files
   * are encrypted server side when stored.
   *
   * @param uuid The Unique User Identifier this is used to locate the users storage location in S3
   * @param path The path to place the object in
   * @param inputStream The stream to write to the S3 Bucket
   * @param contentDisposition Content Disposition of the inputStream
   * @return <li>
   * <ul>{@link Status#OK} on success
   * <ul>{@link Status#NOT_FOUND} if the bucket doesn't exist</ul>
   * <ul>{@link Status#CONFLICT} if the key already exists
   * <ul>{@link Status#NO_CONTENT} on error</ul>
   * </li>
   */
  @Path("/submit-location/{uuid}/")
  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response submit(@PathParam("uuid") String uuid,
                         @QueryParam("path") String path,
                         @FormDataParam("file") InputStream inputStream,
                         @FormDataParam("file") FormDataContentDisposition contentDisposition)
  {
    return create(getUserName() + "/" + (path != null ? path : ""), contentDisposition.getFileName(), inputStream);
  }

  /**
   * Create a "folder" in S3.  This is really just a 0 length key with a / suffix that you can
   * use as a container to place other objects in.
   * @param uuid
   * @param path
   * @return
   */
  @Path("/submit/{uuid}/{path: .*}")
  @PUT
  public Response submit(@PathParam("uuid") String uuid,
                         @PathParam("path") String path)
  {
    return makeFolder(getUserName(), path);
  }

  @Path("/folder/{uuid}")
  @PUT
  public Response makeFolder(@PathParam("uuid") String uuid,
                         @QueryParam("path") String path)
  {
    String userName = getUserName();
    try
    {
      String name;
      int pos = path.lastIndexOf('/');
      if(pos > 0 && path.length() > pos+1){
        name = path.substring(pos+1);
        path = String.format("%s/%s", userName, path.substring(0, pos));
      }
      else {
        name = path;
        path = userName;
      }

      if( !isNameValid(name) ){
        return getInvalidNameResponse(name);
      }
      if( storage.isExists(path, name, true) ){
        return Response.status(Status.CONFLICT).entity("Folder \"" + name + "\" is already exists in the \"" + path + "\"").build();
      }
      else {
        storage.makeFolder(userName, path, name);
      }
    }
    catch (WebApplicationException ex){
      throw ex;
    }
    catch(Exception e)
    {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }

  /**
   * Get a list of user data that are stored in the S3 Bucket.
   *
   * @param uuid The unique user identifier this is used to lookup the proper location in the s3 bucket
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on error.
   */
  @Path("/data/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response dir(@PathParam("uuid") String uuid)
  {
    return list(getUserName(), null);
  }

  /**
   * Get a list of user data that are stored in the S3 Bucket.
   *
   * @param uuid The unique user identifier this is used to lookup the proper location in the s3 bucket
   * @param path A QueryParam that contains a path like structure
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on error.
   */
  @Path("/data-location/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response dir(@PathParam("uuid") String uuid,
                      @QueryParam("path") String path)
  {
    return list(getUserName(), path);
  }

  /**
   * An individual piece of user data identified by the uuid and keyName
   * @param uuid The unique user identifier this is used to lookup the proper location in the s3 bucket
   * @param keyName
   * @return The inputstream of the requested resource or an empty stream
   */
  @Path("/data/{uuid}/{keyName: .*}")
  @GET
  public Response data(@PathParam("uuid") String uuid,
                       @PathParam("keyName") String keyName,
                       @QueryParam("token") String token,
                       @Context HttpServletRequest request)
  {
    String key = String.format("%s/%s", getUserName(), keyName);

    try {
      FileStream object = storage.getFileStream(key, false);
      //object.getObjectMetadata();
      InputStream is = object.getInputStream();
      String[] names = keyName.split("/");
      String fileName = names[names.length - 1];

      String contentType = object.getContentType();


      String authHeader = BEARER_PREFIX + token;
      Map<String, Boolean> gitMatches = projectProvisioningClient.exists(Collections.singleton(fileName), authHeader, securityContext);
      boolean matchWithGitMasterRepo = gitMatches.containsKey(fileName) && gitMatches.get(fileName);
      eventLogger.sendDownloadEvent(
              fileName,
              object.getSize(),
              contentType,
              matchWithGitMasterRepo,
              authHeader,
              request == null ? null : request.getRemoteAddr(),
              securityContext
      );

      return Response.ok(is)
              .type(contentType)
              .header("content-disposition", "attachment; filename = " + fileName)
              .build();
    }
    catch (AmazonS3Exception e){
      if(e.getStatusCode() != 404){
        log.error(e.getMessage(), e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      } else {
        return Responses.notFound().entity("Cannot find \"" + key + "\"").build();
      }
    }
    catch (WebApplicationException e){
      throw e;
    }
    catch(Exception e)
    {
      //System.out.println("--> 2");
      log.error(e.getMessage(), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Path("/zip/{uuid}")
  @GET
  public Response zip(@PathParam("uuid") final String uuidParam,@QueryParam("path") String pathParam,
                       @QueryParam("file") List<String> files, @QueryParam("token") String token, @Context HttpServletRequest request) throws IOException {

    final String path = (pathParam != null) ? pathParam:"";
    final String userFolder = (uuidParam != null) ? getUserName():"";

    Set<String> keyNames = Sets.newHashSet(files);
    String zipName = "ghap";
    if(keyNames.size() == 1){
      String keyName = keyNames.iterator().next();
      if(keyName.endsWith("/")){
        zipName = keyName.substring(0, keyName.length() - 1);
      } else {
        return data(userFolder, (path.isEmpty() ? "":path + "/") + keyName, token, request);
      }
    }

    final String relPath = userFolder + "/" + (path.isEmpty() ? "":path + "/");
    final Function<String, String> keyBuilder = (keyName) -> relPath + keyName;

    final StreamingOutput stream = (os) -> {

      String key = "";
      // check files exists
      for (String keyName : keyNames) {
        key = keyBuilder.apply(keyName);
        if( !storage.isExists(key) ){
          throw new WebApplicationException(Responses.notFound().entity("Cannot find \"" + key + "\"").build());
        }
      }

      try {

        List<FileEvent> fileEvents = new ArrayList<>();

        // process ZIP file
        ZipOutputStream zos = new ZipOutputStream(os);
        for (String keyName : keyNames) {

          key = keyBuilder.apply(keyName);

          if (keyName.endsWith("/")) {
            // process directory
            ObjectListing objectListing = storage.listObjects(key, true);
            Collection<FileInfo> fileList = objectListing.getFiles();
            for (FileInfo fileInfo : fileList) {
              FileStream object = storage.getFileStream(fileInfo.getPath(), true);
              try (InputStream objectStream = object.getInputStream()) {
                String entryName = fileInfo.getPath().replace(storage.getRootPath(), "");
                entryName = entryName.substring(relPath.length());
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                IOUtils.copy(objectStream, zos);
              } finally {
                fileEvents.add(new FileEvent(fileInfo.getName(), object.getSize(), object.getContentType()));
              }
            }
          } else {
            String[] names = keyName.split("/");

            ZipEntry zipEntry = new ZipEntry(names[names.length - 1]);
            zos.putNextEntry(zipEntry);
            FileStream object = storage.getFileStream(key, false);
            try (InputStream objectStream = object.getInputStream()) {
              IOUtils.copy(objectStream, zos);
              fileEvents.add(new FileEvent(new File(key).getName(), object.getSize(), object.getContentType()));
            }
          }
        }
        zos.close();

        sendDownloadEvents(fileEvents, token, request.getRemoteAddr());
      } catch (Exception e){
        log.error("Current relative path: \"" + key + "\". Error: " + e.getMessage(), e);
        throw new WebApplicationException();
      }
    };




    return Response.ok(stream).type("application/zip").header("content-disposition", "attachment; filename = " + zipName + ".zip").build();
  }

  private void sendDownloadEvents(List<FileEvent> fileEvents, String token, String remoteAddr) throws IOException {
    String authHeader = BEARER_PREFIX + token;
    Set<String> fileNameSet = fileEvents.stream().map(FileEvent::getName).collect(Collectors.toSet());
    Map<String, Boolean> gitMatches = projectProvisioningClient.exists(fileNameSet, authHeader, securityContext);

    for(FileEvent f:fileEvents){
      boolean matchWithGitMasterRepo = gitMatches.containsKey(f.getName()) && gitMatches.get(f.getName());

      eventLogger.sendDownloadEvent(
              f.getName(),
              f.getSize(),
              f.getContentType(),
              matchWithGitMasterRepo,
              authHeader,
              remoteAddr,
              securityContext
      );
    }
  }

  /**
   * Delete an object from the S3 Bucket identified by uuid and keyName
   * @param uuid The unique user identifier this is used to lookup the proper location in the s3 bucket
   * @param path
   * @param files
   * @return {@link Status#OK} on success and {@link Status#NOT_FOUND} on error
   */
  @Path("/delete/{uuid}")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)

  public Response delete(@PathParam("uuid") String uuid, @QueryParam("path") String path,
                         @QueryParam("file") List<String> files) throws IOException {
    if (path == null) {
      path = "";
    }

    Set<String> keyNames = Sets.newHashSet(files);
    String key = null;
    try {
      String relPath = getUserName() + "/" + (path.isEmpty() ? "" : path + "/");
      for (String keyName : keyNames) {
        storage.deleteObjects(relPath + keyName);
      }
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() != 404) {
        log.error(e.getMessage(), e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      } else {
        return Responses.notFound().entity("Cannot find \"" + key + "\"").build();
      }
    }

    return Response.ok().build();
  }

  /**
   * An folder as zip archive
   * @param uuid The unique user identifier this is used to lookup the proper location in the s3 bucket
   * @param keyName
   * @return The inputstream of the requested resource or an empty stream
   */
  @Path("/dataFolder/{uuid}/{keyName: .*}")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response dataFolder(@PathParam("uuid") String uuid,
                       @PathParam("keyName") String keyName) {
    String root = getUserName();
    String keyPath = String.format("%s/%s", root, keyName);
    String[] names = keyName.split("/");
    InputStream inputStream;
    try {
      inputStream = storage.loadFolder(keyPath, root);
    } catch (IOException e) {
      log.error("error generate zip file for key " + keyName, e);
      throw new WebApplicationException(e);
    }
    return Response.ok(inputStream).header("content-disposition", "attachment; filename = " + names[names.length - 1] + ".zip").build();
  }
  
  public String getServiceName() 
  {
    return userDataName;
  }

  private String getUserName(){
    return securityContext.getUserPrincipal().getName();
  }

  /**
   * Get a listing of all of the content stored at a specific "location" in s3.  A "location"
   * is simply a keyname.
   * @param path
   * @return
   */
  private Response list(String userFolder, String path)
  {
    try
    {
      String prefix = userFolder + "/" + Utils.correctPath(path, true);

      ObjectListing listing = storage.listObjects(prefix, false);

      Collection<FileInfo> fileList = listing.getFiles();
      Collection<FileInfo> folderList = listing.getFolders();

      if(folderList.isEmpty() && fileList.isEmpty()) {
        return Response.ok(EMPTY_LIST).build();
      }

      //int pathStartFrom = storage.getRootPath().length() + userFolder.length() + 1;
      int nameStartFrom = storage.getRootPath().length() + prefix.length();

      List<UserData> folders = new ArrayList<>(folderList.size());
      for(FileInfo folder:folderList){
        String name = folder.getName();
        folders.add(new UserData(path, name, true, null, null));
      }
      Collections.sort(folders);

      List<UserData> files = new ArrayList<>(folderList.size());
      for(FileInfo file:fileList){
        String name = file.getName();
        if( name.length() > 0 )
          files.add(new UserData(path, name, false, file.getSize(), file.getLastModified() ));
      }
      Collections.sort(files);

      List<UserData> userdata = new ArrayList(folders.size() + files.size());
      userdata.addAll(folders);
      userdata.addAll(files);

      Gson gson = new Gson();
      String jsonUserData = gson.toJson(userdata);
      return Response.ok(jsonUserData).build();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Create an object in S3
   * @param path The path of the object
   * @param path The name of the object
   * @param inputStream The content of the object
   * @return
   */
  private Response create(String path, String name, InputStream inputStream)
  {
    try {
      if( !isNameValid(name) ){
        return getInvalidNameResponse(name);
      }

      // bad practice to use an exception to control program
      // flow, but there isn't another way to check if the
      // key exists.
      if( storage.isExists(path, name, false) ) {
        return Response.status(Status.CONFLICT).build();
      }

      String userName = getUserName();
      File file = File.createTempFile("user_", ".data");
      try {
        FileUtils.copyInputStreamToFile(inputStream, file);
        storage.putObject(userName, path, name, file);
      } finally {
        file.delete();
      }

      return Response.ok().build();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isNameValid(String name){
    return NAME_PATTERN.matcher(name).matches();
  }

  private Response getInvalidNameResponse(String name){
    return Response.status(Status.BAD_REQUEST).entity("File name \"" + name + "\" contains invalid characters(the following characters are not allowed: " + INVALID_CHARS+")").build();
  }
}
