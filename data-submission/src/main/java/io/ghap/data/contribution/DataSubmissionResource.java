package io.ghap.data.contribution;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import io.ghap.data.logevents.LogEventsClient;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("DataSubmission")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "BMGF Administrator", "Administrators", "Data Curator", "Data Contributor"}, predicateType = PredicateType.OR)
public class DataSubmissionResource
{
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private static final String BEARER_PREFIX = "Bearer ";

  @Configuration("datasubmission.name")
  private String dataSubmissionName;

  @Inject
  private LogEventsClient logEventsClient;

  private final ExecutorService submissionExecutor = Executors.newCachedThreadPool();

  /**
   * Contribute a file to the DataSubmission S3 bucket all files
   * are encrypted server side when stored.
   * 
   * @param inputStream The stream to write to the S3 Bucket
   * @param contentDisposition Content Disposition of the inputStream
   * @return <li>
   * <ul>{@link Status#OK} on success
   * <ul>{@link Status#NOT_FOUND} if the bucket doesn't exist</ul>
   * <ul>{@link Status#CONFLICT} if the key already exists 
   * <ul>{@link Status#NO_CONTENT} on error</ul>
   * </li>
   */
  @Path("/submit")
  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response contribute(@FormDataParam("file") InputStream inputStream,
                             @FormDataParam("file") FormDataContentDisposition contentDisposition,
                             @Context SecurityContext securityContext)
  {
    String bucketName = ConfigurationManager.getConfigInstance().getString("datasubmission.s3.bucketname");
    String keyName = contentDisposition.getFileName();
    try
    {
      AmazonS3Client s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
      metadata.setContentType(contentDisposition.getType());
      //metadata.setContentLength(contentDisposition.getSize());


      OAuthPrincipal user = (OAuthPrincipal)securityContext.getUserPrincipal();
      Map<String, String> userMeta = new HashMap<>();
      userMeta.put("username", user.getName());
      metadata.setUserMetadata(userMeta);

      if(!s3.doesBucketExist(bucketName))
      {
        return Response.status(Status.NOT_FOUND).build();
      }
      
      boolean exists = false;
      
      // bad practice to use an exception to control program
      // flow, but there isn't another way to check if the
      // key exists.
      try
      {
        ObjectMetadata data = s3.getObjectMetadata(bucketName, keyName);
        exists = true;
        return Response.status(Status.CONFLICT).build();
      } 
      catch(AmazonServiceException ase)
      {// eat it
      }
      
      if(!exists)
      {
        File file = storeSubmissionContentsIntoFile(inputStream);

        submissionExecutor.submit(() -> {
          putSubmissionContentsIntoTarget(file, metadata, bucketName, keyName);
          return null;
        });

      }
      return Response.ok().build();
    }
    catch(Exception e)
    {
      if(log.isErrorEnabled())
      {
        log.error(e.getMessage() + ". Bucket: \"" + bucketName + "\", Key: \"" + keyName + "\"", e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private File storeSubmissionContentsIntoFile(InputStream inputStream) throws IOException {

    log.debug("About to create temporary file for upload");

    BufferedOutputStream bos = null;
    File file = null;
    try {
      file = File.createTempFile("user", "data");
      FileOutputStream fos = new FileOutputStream(file);
      bos = new BufferedOutputStream(fos);
      byte[] buffer = new byte[8 * 1024];
      for (int read = inputStream.read(buffer); read > 0; read = inputStream.read(buffer)) {
        bos.write(buffer, 0, read);
      }


    } finally {
      try {
        bos.flush();
      } finally {
        bos.close();
      }
      log.debug("Finished creating temporary file for upload");
    }
    return file;
  }

  private void putSubmissionContentsIntoTarget(
          File submissionContentsFile, ObjectMetadata metadata,
          String bucketName, String keyName) throws IOException, InterruptedException {


    log.debug(String.format("About to upload <%s> - transfer manager", keyName));


    TransferManager tm = new TransferManager(new DefaultAWSCredentialsProviderChain());

    try {
      // TransferManager processes all transfers asynchronously,
      // so this call will return immediately.
      PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, keyName, submissionContentsFile).withMetadata(metadata);
      Upload upload = tm.upload(putObjectRequest);
      //Upload upload = tm.upload(bucketName, keyName, submissionContentsFile);

      upload.waitForUploadResult();

    } finally {
      log.debug(String.format("Finished upload <%s> - transfer manager", keyName));
      //TODO process result somehow
      submissionContentsFile.delete();
    }
  }

  /**
   * Get a list of submissions that are stored in the S3 Bucket.
   * 
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on error.
   */
  @Path("/submissions")
  @GET
  public Response submissions()
  {
    try
    {
      AmazonS3Client s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
      String bucketName = ConfigurationManager.getConfigInstance().getString("datasubmission.s3.bucketname");
      
      ObjectListing listing = s3.listObjects(bucketName);
      List<S3ObjectSummary> summaries = listing.getObjectSummaries();
      List<Submission> submissions = new ArrayList<Submission>();
      for(S3ObjectSummary summary : summaries) 
      {
        submissions.add(new Submission(summary));
      }
      S3Utils.retrieveMetadata(s3, bucketName, submissions);
      Gson gson = new Gson();
      String jsonSubmissions = gson.toJson(submissions);
      return Response.ok(jsonSubmissions).build();
    }
    catch(Exception e)
    {
      if(log.isErrorEnabled())
      {
        log.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Get a data submission identified by the given keyName
   * @param keyName
   * @return The inputstream of the requested resource or an empty stream
   */
  @Path("/submission/{keyName}")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public InputStream submission(@PathParam("keyName") String keyName, @QueryParam("token") String token, @Context HttpServletRequest request, @Context SecurityContext securityContext)
  {
    String bucketName = ConfigurationManager.getConfigInstance().getString("datasubmission.s3.bucketname");
    
    AmazonS3Client s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());

    try {
      //ObjectMetadata metaData = s3.getObjectMetadata(bucketName, keyName);
      S3Object object = s3.getObject(bucketName, keyName);
      S3ObjectInputStream objectStream = object.getObjectContent();
      logEventsClient.sendDownloadEventAsync(
              keyName,
              object.getObjectMetadata().getContentLength(),
              object.getObjectMetadata().getContentType(),
              BEARER_PREFIX + token,
              request.getRemoteAddr(),
              securityContext
      );
      return objectStream;
    }
    catch (WebApplicationException e){
      throw e;
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return new ByteArrayInputStream(new byte[0]);
    }
  }
  
  /**
   * Delete an object from the S3 Bucket identified by keyName
   * @param keyName
   * @return {@link Status#OK} on success and {@link Status#NOT_FOUND} on error
   */
  @Path("/delete/{keyName}")
  @DELETE
  public Response delete(@PathParam("keyName") String keyName)
  {
    try
    {
      String bucketName = ConfigurationManager.getConfigInstance().getString("datasubmission.s3.bucketname");
      
      AmazonS3Client s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
      s3.deleteObject(bucketName, keyName);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      if(log.isErrorEnabled())
      {
        log.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_FOUND).build();
    }
  }
  
  public String getServiceName() 
  {
    return dataSubmissionName;
  }
}
