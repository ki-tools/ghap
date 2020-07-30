package io.ghap.provision.vpg;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.github.hburgmeier.jerseyoauth2.api.user.IUser;
import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.OAuthUser;
import io.ghap.oauth.PredicateType;
import io.ghap.provision.vpg.data.ActivityExistence;
import io.ghap.provision.vpg.data.ActivityStatus;
import io.ghap.provision.vpg.data.VPGMultiFactory;
import io.ghap.provision.vpg.data.VirtualResource;

import io.ghap.util.Iso8601;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import javax.persistence.RollbackException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;

import static io.ghap.provision.vpg.Utils.getStackId;
import static io.ghap.provision.vpg.Utils.getUserId;
import static io.ghap.provision.vpg.Utils.getVirtualResource;

@Singleton
@Path("MultiVirtualPrivateGrid")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class MultiVirtualPrivateGridResource
{
  private Logger logger = LoggerFactory.getLogger(MultiVirtualPrivateGridResource.class);
  
  @Inject
  private VPGMultiFactory vpgFactory;

  @Configuration("multi.vpg.name")
  private String vpgName;

  @Context
  private SecurityContext securityContext;

  /**
   * Create a new Stack for the provided user.  Only one stack
   * per user is allowed.  If one already exists {@link Status#NO_CONTENT}
   * is returned otherwise {@link Status#OK}
   * @param uuid_str The user unique id to create a stack for.
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>

   * @return
   */
  @Path("/create/{uuid}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("uuid") String uuid_str, Activity activity)
  {
    try
    {
      Principal principal = securityContext.getUserPrincipal();
      String email = "UNKNOWN";
      if(principal instanceof OAuthPrincipal) {
        OAuthPrincipal oAuthPrincipal = (OAuthPrincipal)principal;
        IUser user = oAuthPrincipal.getUser();
        if(user instanceof OAuthUser) {
          OAuthUser oAuthUser = (OAuthUser)user;
          email = oAuthUser.getEmail();
        }
      }
      UUID uuid = UUID.fromString(uuid_str);

      Gson gson = new Gson();
      if (vpgFactory.exists(uuid, activity)) {
        throw new WebApplicationException(Response.status(Status.CONFLICT).entity(gson.toJson(
                ImmutableMap.of(
                        "message", "Activity already exists",
                        "existingActivities", Arrays.asList(activity)
                )
        )).build());
      }

      VirtualPrivateGrid vpg = vpgFactory.create(principal.getName(), email, uuid, activity);

      String jsonString = gson.toJson(vpg);
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Create a new Stack for the provided user.  Only one stack
   * per user is allowed.  If one already exists {@link Status#NO_CONTENT}
   * is returned otherwise {@link Status#OK}
   * @param uuid_str The user unique id to create a stack for.
   * @param activities <code>
   * [
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * },
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f06000",
   *  "activityName":"Another Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * ]
   * </code>

   * @return
   */
  @Path("/create-multiple/{uuid}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("uuid") String uuid_str, Activity[] activities)
  {
    try
    {
      Principal principal = securityContext.getUserPrincipal();

      UUID uuid = UUID.fromString(uuid_str);
      String email = "UNKNOWN";
      if(principal instanceof OAuthPrincipal) {
        OAuthPrincipal oAuthPrincipal = (OAuthPrincipal)principal;
        IUser user = oAuthPrincipal.getUser();
        if(user instanceof OAuthUser) {
          OAuthUser oAuthUser = (OAuthUser)user;
          email = oAuthUser.getEmail();
        }
      }

      Gson gson = new Gson();
      Set<Activity> existedActivities = new HashSet<>();
      if (activities != null) {
        for (Activity activity : activities) {
          if (vpgFactory.exists(uuid, activity)) {
            existedActivities.add(activity);
          }
        }
      }
      if( !existedActivities.isEmpty() ){
        throw new WebApplicationException(Response.status(Status.CONFLICT).entity(gson.toJson(
                ImmutableMap.of(
                        "message", "Activity already exists",
                        "existingActivities", existedActivities
                )
        )).build());
      }

      VirtualPrivateGrid[] vpgs;
      try {
        vpgs = vpgFactory.create(principal.getName(), email, uuid, activities);
      } catch (RollbackException ex){
        StringBuilder sb = new StringBuilder(ex.toString());
        Throwable curCause = ex;
        while(ex.getCause() != null && curCause != ex.getCause()){
          curCause = ex.getCause();
          sb.append("\n").append(curCause.toString());
        }
        throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(
                ImmutableMap.of(
                        "message", "Some inconsistency in DB during VPG creation",
                        "details", sb.toString()
                )
        )).build());
      }


      String jsonString = gson.toJson(vpgs);
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Get the stack for the provided user id.
   * @param uuid_str The unique user id
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return
   */
  @Path("/get-by-activity/{uuid}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("uuid") String uuid_str, Activity activity)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      VirtualPrivateGrid vpg = vpgFactory.get(uuid, activity);
      if(vpg == null){
        return Response.status(Status.NOT_FOUND).build();
      }
      Gson gson = new Gson();
      String jsonString = gson.toJson(vpg);
      return Response.ok(jsonString).build();
    }
    catch(NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Check the status of a stack identified by the user id.
   * The status is anyone of the following status codes
   * returned by cloudformation:
   * <ul>
   * <li>CREATE_IN_PROGRESS</li>
   * <li>CREATE_FAILED</li>
   * <li>CREATE_COMPLETE</li>
   * <li>ROLLBACK_IN_PROGRESS</li>
   * <li>ROLLBACK_FAILED</li>
   * <li>ROLLBACK_COMPLETE</li>
   * <li>DELETE_IN_PROGRESS</li>
   * <li>DELETE_FAILED</li>
   * <li>DELETE_COMPLETE</li>
   * <li>UPDATE_IN_PROGRESS</li>
   * <li>UPDATE_COMPLETE_CLEANUP_IN_PROGRESS</li>
   * <li>UPDATE_COMPLETE</li>
   * <li>UPDATE_ROLLBACK_IN_PROGRESS</li>
   * <li>UPDATE_ROLLBACK_FAILED</li>
   * <li>UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS</li>
   * <li>UPDATE_ROLLBACK_COMPLETE</li>
   * </ul>
   * @param uuid_str
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return
   */
  @Path("/status/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response status(@PathParam("uuid") String uuid_str, Activity activity)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      String status = vpgFactory.status(uuid, activity);
      Gson gson = new Gson();
      String jsonString = gson.toJson(status);
      return Response.ok(jsonString).build();
    }
    catch(NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_FOUND).build();
    }
  }


  /**
   * Check the status of a stack identified by the user id.
   * The status is anyone of the following status codes
   * returned by cloudformation:
   * <ul>
   * <li>CREATE_IN_PROGRESS</li>
   * <li>CREATE_FAILED</li>
   * <li>CREATE_COMPLETE</li>
   * <li>ROLLBACK_IN_PROGRESS</li>
   * <li>ROLLBACK_FAILED</li>
   * <li>ROLLBACK_COMPLETE</li>
   * <li>DELETE_IN_PROGRESS</li>
   * <li>DELETE_FAILED</li>
   * <li>DELETE_COMPLETE</li>
   * <li>UPDATE_IN_PROGRESS</li>
   * <li>UPDATE_COMPLETE_CLEANUP_IN_PROGRESS</li>
   * <li>UPDATE_COMPLETE</li>
   * <li>UPDATE_ROLLBACK_IN_PROGRESS</li>
   * <li>UPDATE_ROLLBACK_FAILED</li>
   * <li>UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS</li>
   * <li>UPDATE_ROLLBACK_COMPLETE</li>
   * </ul>
   * @param uuid_str
   * @param activities <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return activityStatuses
   * <code>
   * {
   *  [
   *    {
   *      "uuid" : "6053ff52-2598-4999-b4ff-b83617f05830",
   *      "status" : "CREATE_COMPLETE"
   *    },
   *    {
   *      "uuid" : "6053ff52-2598-4999-b4ff-b83617f0600",
   *      "status" : "CREATE_COMPLETE"
   *    }
   *  ]
   * }
   * </code>
   */
  @Path("/statuses/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response statuses(@PathParam("uuid") String uuid_str, Activity[] activities)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      ActivityStatus[] statuses = vpgFactory.statuses(uuid, activities);
      Gson gson = new Gson();
      String jsonString = gson.toJson(statuses);
      return Response.ok(jsonString).build();
    }
    catch(NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * Get an RDP file for an Instance
   * @param virtualResource
   * @return
   */
  @Path("/rdp")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public InputStream rdp(VirtualResource virtualResource)
  {
    InputStream inputStream = null;
    String rdp = "";
    Principal principal = securityContext.getUserPrincipal();

    String os = virtualResource.getInstanceOsType();
    if(os != null && os.equalsIgnoreCase("windows"))
    {
      rdp = vpgFactory.rdp(virtualResource, principal.getName());
    }

    try
    {
      inputStream = new ByteArrayInputStream(rdp.getBytes());
    }
    catch(Exception e)
    {
      inputStream = new ByteArrayInputStream(new byte[] {});
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
    }
    return inputStream;
  }

  /**
   * Get an RDP file for an Instance
   * @param instanceId The instance id of the VirtualResource
   * @param os The os of the VirtualResource
   * @param ipAddress The ip address of the VirtualResource
   * @return
   */
  @Path("/rdp-file/{instanceId}/{os}")
  @GET
  @Produces({"application/rdp"})
  public Response rdp(@PathParam("instanceId") String instanceId,
                      @PathParam("os") String os,
                      @QueryParam("ipAddress") String ipAddress,
                      @QueryParam("dnsName") String dnsName)
  {
    // we cannot use "ipAddress" as a path param. See: http://stackoverflow.com/questions/3856693/a-url-resource-that-is-a-dot-2e
    String rdp = "";
    Principal principal = securityContext.getUserPrincipal();
    
    if(os != null && os.equalsIgnoreCase("windows"))
    {
      if( StringUtils.isNotBlank(dnsName)) { //use provided dns name
        logger.info("MultiVirtualPrivateGridResource.rdp, dns name provided {}", dnsName);
        rdp = vpgFactory.rdp(dnsName, principal.getName());
      } else { //try to get dns name from ec2client
        dnsName = vpgFactory.getCurrentPublicDNSname(instanceId);
        if( StringUtils.isNotBlank(dnsName)) {
          logger.info("MultiVirtualPrivateGridResource.rdp, dns name from ec2 is {}", dnsName);
          rdp = vpgFactory.rdp(dnsName, principal.getName());
        } else { //use provided ip address as a last resort
          logger.info("MultiVirtualPrivateGridResource.rdp, dns name not available, using ip {}", ipAddress);
          rdp = vpgFactory.rdp(ipAddress, principal.getName());
        }
      }
    }
    Response response = Response.ok(new ByteArrayInputStream(rdp.getBytes()))
        .header("Content-Disposition", String.format("attachment;filename=%s.rdp", instanceId))
        .header("Expires", 0)
        .header("Cache-Control","must-revalidate")
        .header("Pragma", "public")
        .build();
    return response;
  }
  
  
  

  /**
   * Get the pem file for a stack.  This is the file that
   * allows to login to the machine without providing a
   * password.
   * 
   * @param uuid_str The unique user id
   * @param auid_str The unique activity id
   * @return
   */
  @Path("/pem/{uuid}/{auid}")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Consumes(MediaType.APPLICATION_JSON)
  public InputStream pem(@PathParam("uuid") String uuid_str, @PathParam("auid") String auid_str)
  {
    InputStream inputStream = null;
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      Activity activity = new Activity(auid_str);

      VirtualPrivateGrid vpg = vpgFactory.get(uuid, activity);
      inputStream = new ByteArrayInputStream(vpg.getPemKey().getBytes());
    }
    catch(Exception e)
    {
      inputStream = new ByteArrayInputStream(new byte[] {});
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
    }
    return inputStream;
  }
  
  /**
   * Get all of the stacks that currently exist.
   * @return
   */
  @Path("/get")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get()
  {
    try
    {
      List<VirtualPrivateGrid> vpgs = vpgFactory.get();
      Gson gson = new Gson();
      String jsonString = gson.toJson(vpgs);
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Get the stack for the provided user id.
   * @param uuid_str The unique user id
   * @return
   */
  @Path("/get/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("uuid") String uuid_str)
  {
    List<VirtualPrivateGrid> vpgs = null;
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      vpgs = vpgFactory.get(uuid);
      if(vpgs == null){
        vpgs = new ArrayList<VirtualPrivateGrid>();
      }
    }
    catch(NoResultException e){
      vpgs = new ArrayList<VirtualPrivateGrid>();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    Gson gson = new Gson();
    String jsonString = gson.toJson(vpgs);
    return Response.ok(jsonString).build();
  }

  /**
   * Get the compute resources that are currently provisioned
   * @return {@link Status#OK} on success with a JSON List of VirtualResources or
   * {@link Status#NO_CONTENT} on error
   */
  @Path("/get/resources/aws")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAwsResources()
  {
    try
    {
      List<VirtualResource> resources = vpgFactory.getVirtualResources(false);
      Gson gson = new GsonBuilder()
              .registerTypeAdapter(Date.class, Iso8601.getJsonSerializer())
              .registerTypeAdapter(Date.class, Iso8601.getJsonDeserializer()).create();

      String jsonString = gson.toJson(resources.toArray(new VirtualResource[resources.size()]));
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Get the compute resources that are currently provisioned
   * @return {@link Status#OK} on success with a JSON List of VirtualResources or
   * {@link Status#NO_CONTENT} on error
   */
  @Path("/get/user/compute")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getComputeResources()
  {
    try
    {
      List<VirtualResource> resources = vpgFactory.getVirtualResources(true);
      Gson gson = new GsonBuilder()
              .registerTypeAdapter(Date.class, Iso8601.getJsonSerializer())
              .registerTypeAdapter(Date.class, Iso8601.getJsonDeserializer()).create();

      String jsonString = gson.toJson(resources.toArray(new VirtualResource[resources.size()]));
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Get the compute resources that are associated with a given user id
   * @param user_uuid The UUID of the VirtualPrivateGrid
   * @return {@link Status#OK} on success with a JSON List of VirtualResources or
   * {@link Status#NO_CONTENT} on error
   */
  @Path("/get/user/compute/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUsersComputeResources(@PathParam("uuid") String user_uuid)
  {
    try
    {
      UUID uuid = UUID.fromString(user_uuid);
      List<VirtualResource> resources = vpgFactory.getVirtualResources(uuid);

      Gson gson = new Gson();
      String jsonString = gson.toJson(resources.toArray(new VirtualResource[resources.size()]));
      return Response.ok(jsonString).build();
    }
    catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Check server logs please. Error: " + e.getMessage()).build();
    }
  }

  /**
   * Get the compute resources that are associated with a given stack id
   * @param vpguid The UUID of the VirtualPrivateGrid
   * @return {@link Status#OK} on success with a JSON List of VirtualResources or
   * {@link Status#NO_CONTENT} on error
   */
  @Path("/get/compute/{vpguid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getComputeResources(@PathParam("vpguid") String vpguid)
  {
    try
    {
      UUID uuid = UUID.fromString(vpguid);
      VirtualPrivateGrid vpg = vpgFactory.getByVpgId(uuid);
      List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);

      Gson gson = new Gson();
      String jsonString = gson.toJson(resources.toArray(new VirtualResource[0]));
      return Response.ok(jsonString).build();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Check if a stack exists for the provided user id.
   * @param uuid_str The unique user id.
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return
   */
  @Path("/exists/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response exists(@PathParam("uuid") String uuid_str, Activity activity)
  {
    UUID uuid = UUID.fromString(uuid_str);
    if(vpgFactory.exists(uuid, activity))
    {
      return Response.ok().build();
    }
    else
    {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * Check if a stack exists for the provided user id.
   * @param uuid_str The unique user id.
   * @param activities <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return activityExistences
   * <code>
   * {
   *  [
   *    {
   *      "uuid" : "6053ff52-2598-4999-b4ff-b83617f05830",
   *      "existence" : "true"
   *    },
   *    {
   *      "uuid" : "6053ff52-2598-4999-b4ff-b83617f0600",
   *      "existence" : "true"
   *    }
   *  ]
   * }
   * </code>
   */
  @Path("/existences/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response existences(@PathParam("uuid") String uuid_str, Activity[] activities)
  {
    UUID uuid = UUID.fromString(uuid_str);
    ActivityExistence[] existences = vpgFactory.existences(uuid, activities);
    Gson gson = new Gson();
    String jsonString = gson.toJson(existences);
    return Response.ok(jsonString).build();
  }

  /**
   * Destroy the stack for the provided user id and activity
   * @param uuid_str
   * @param activity_uid
   * @return
   */
  @Path("/terminate/{uuid}/{activity_uid}")
  @DELETE
  public Response terminate(@PathParam("uuid") String uuid_str, @PathParam("activity_uid") String activity_uid)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      UUID aguid = UUID.fromString(activity_uid);
      boolean isExistedAndDeleted = vpgFactory.delete(uuid, aguid);
      if(!isExistedAndDeleted){
        return Response.status(Status.NOT_MODIFIED).entity("Cannot find instance for decommissioning").build();
      }
    }
    catch(Exception e)
    {
      logger.error(e.getMessage(), e);
      return Response.status(Status.NOT_MODIFIED).entity(e.getMessage()).build();
    }
    return Response.ok().build();
  }

  /**
   * Get an RDP file for an Instance
   * @param resource
   * @return {@link Status#OK} on success with a JSON String containing the Console output of a VirtualResource or
   * {@link Status#NO_CONTENT} on error
   */
  @Path("/console")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response console(VirtualResource resource)
  {
    try {
      String console = vpgFactory.console(resource);
      return Response.ok(console).build();
    }
    catch(Exception e)
    {
      logger.error(e.getMessage(), e);
      return Response.status(Status.NO_CONTENT).entity(e.getMessage()).build();
    }
  }

  /**
   * Pauses a running stack for the provided user id and activity.
   * @param uuid_str
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return
   */
  @Path("/pause/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response pause(@PathParam("uuid") String uuid_str, Activity activity)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      boolean isExistedAndPaused = vpgFactory.pause(uuid, activity);
      if(!isExistedAndPaused){
        return Response.status(Status.NOT_MODIFIED).entity("Cannot find instance for pause").build();
      }
    }
    catch(Exception e)
    {
      logger.error(e.getMessage(), e);
      return Response.status(Status.NOT_MODIFIED).entity(e.getMessage()).build();
    }

    return Response.ok().build();
  }

  /**
   * Resumes a stopped stack for the provided user id and activity.
   * @param uuid_str
   * @param activity <code>
   * {
   *  "uuid":"6053ff52-2598-4999-b4ff-b83617f05830",
   *  "activityName":"An Activity",
   *  "minimumComputationalUnits":2,
   *  "maximumComputationalUnits":10,
   *  "defaultComputationalUnits":1,
   *  "templateUrl":"http://some/s3/location",
   *  "os":"Linux"
   * }
   * </code>
   * @return
   */
  @Path("/resume/{uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response resume(@PathParam("uuid") String uuid_str, Activity activity)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      boolean isExistedAndResumed = vpgFactory.resume(uuid, activity);
      if(!isExistedAndResumed){
        return Response.status(Status.NOT_MODIFIED).entity("Cannot find instance for resume").build();
      }
    }
    catch(Exception e)
    {
      logger.error(e.getMessage(), e);
      return Response.status(Status.NOT_MODIFIED).entity(e.getMessage()).build();
    }

    return Response.ok().build();
  }

  public String getServiceName()
  {
    return vpgName;
  }
}
