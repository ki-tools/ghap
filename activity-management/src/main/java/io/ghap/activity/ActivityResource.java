package io.ghap.activity;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import io.ghap.activity.data.ActivityFactory;

import java.util.List;
import java.util.UUID;

import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("Activity")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class ActivityResource
{
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  private ActivityFactory aFactory;
  
  /**
   * Create an Activity with the given parameters
   * @param activityName The name of the Activity
   * @param minimumSize The minimal number of computational units that will be provisioned
   * @param maximumSize The maximum number of computational units that the activity can consume.  
   * This is essentially an upper bound on the autoscaling configuration
   * @param defaultSize The initial number of computational units that will be started.  This is
   * typically the same as minimum.
   * @return {@link Status#OK} on success or {@link Status#NO_CONTENT} on failure.
   */
  @Path("/create/{activityName}/{minimum}/{maximum}/{default}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("activityName") String activityName,
                      @PathParam("minimum") int minimumSize,
                      @PathParam("maximum") int maximumSize,
                      @PathParam("default") int defaultSize,
                      @QueryParam("templateUrl") String templateUrl)
  {
    try
    {
      Activity activity = aFactory.create(activityName, defaultSize, minimumSize, maximumSize, templateUrl);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
      return Response.ok(jsonActivity).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Create an Activity with the given parameters
   * @param activityName The name of the Activity
   * @param minimumSize The minimal number of computational units that will be provisioned
   * @param maximumSize The maximum number of computational units that the activity can consume.
   * This is essentially an upper bound on the autoscaling configuration
   * @param defaultSize The initial number of computational units that will be started.  This is
   * typically the same as minimum.
   * @return {@link Status#OK} on success or {@link Status#NO_CONTENT} on failure.
   */
  @Path("/create/{activityName}/{minimum}/{maximum}/{default}/{os}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("activityName") String activityName,
                      @PathParam("minimum") int minimumSize,
                      @PathParam("maximum") int maximumSize,
                      @PathParam("default") int defaultSize,
                      @PathParam("os") String os,
                      @QueryParam("templateUrl") String templateUrl)
  {
    try
    {
      Activity activity = aFactory.create(activityName, defaultSize, minimumSize, maximumSize, os, templateUrl);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
      return Response.ok(jsonActivity).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Create an Activity from the provided Activity
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
   * @return {@link Status#OK} on success or {@link Status#NO_CONTENT} on failure.
   */
  @Path("/create")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response get(Activity activity)
  {
    try
    {
      Activity activity_entity = aFactory.create(activity);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity_entity);
      return Response.ok(jsonActivity).build();
    }
    catch(Exception e)
    {
      log.error("Error during Activity creation", e);
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
  
  /**
   * Get an Activity with the given name
   * @param activityName
   * @return {@link Status#OK} on success or {@link Status#NOT_FOUND} on failure.
   */
  @Path("/get/name/{activityName}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
  public Response get(@PathParam("activityName") String activityName)
  {
    try
    {
      Activity activity = aFactory.get(activityName);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
  
      return Response.ok(jsonActivity).build();
    }
    catch (NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      log.error("Cannot retrieve activity by name \"{}\"", activityName, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Get an Activity with the given UUID
   * @param uuid
   * @return {@link Status#OK} on success or {@link Status#NOT_FOUND} on failure.
   */
  @Path("/get/id/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
  public Response getByUUID(@PathParam("uuid") String uuid)
  {
    try
    {
      UUID uid = UUID.fromString(uuid);
      Activity activity = aFactory.get(uid);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
  
      return Response.ok(jsonActivity).build();
    }
    catch (NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      log.error("Cannot retrieve activity by id \"{}\"", uuid, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Get all Activities 
   * @return {@link Status#OK} on success or {@link Status#NOT_FOUND} on failure.
   */
  @Path("/get")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
  public Response get() 
  {
    try
    {
      List<Activity> activity = aFactory.get();
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
  
      return Response.ok(jsonActivity).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_FOUND).build();
    }
  }
  
  /**
   * Update the given Activity
   * @param activity
   * @return {@link Status#OK} on success or {@link Status#NOT_MODIFIED} on failure.
   */
  @PUT
  @Path("/update")
  @Consumes("application/json")
  @Produces("application/json")
  public Response update(Activity activity)
  {
    try
    {
      aFactory.update(activity);
      Gson gson = new Gson();
      String jsonActivity = gson.toJson(activity);
      return Response.ok(jsonActivity).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }
  
  /**
   * Delete the given Activity
   * @param activity
   * @return {@link Status#OK} on success or {@link Status#NOT_MODIFIED} on failure.
   */
  @DELETE
  @Path("/delete/activity")
  @Consumes("application/json")
  public Response delete(Activity activity)
  {
    try
    {
      aFactory.delete(activity);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }
 
  /**
   * Delete an Activity identified by UUID
   * @param uuid
   * @return {@link Status#OK} on success or {@link Status#NOT_MODIFIED} on failure.
   */
  @DELETE
  @Path("/delete/id/{uuid}")
  public Response delete(@PathParam("uuid") String uuid)
  {
    try
    {
      UUID uid = UUID.fromString(uuid);
      aFactory.delete(uid);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }
}
