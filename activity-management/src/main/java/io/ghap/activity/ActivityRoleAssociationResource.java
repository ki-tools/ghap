package io.ghap.activity;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import io.ghap.activity.data.ActivityFactory;
import io.ghap.activity.data.AssociationFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
@Path("ActivityRoleAssociation")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class ActivityRoleAssociationResource
{
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  private ActivityFactory activityFactory;
  
  @Inject
  private AssociationFactory associationFactory;
  
  /**
   * Create an ActivityRoleAssociation associating an activity
   * identified by a given name, and a UUID string that corresponds
   * to the id of a Role.
   * @param activityName The name of the activity to assign to a Role
   * @param role_uuid The role id to assign the activity to
   * @return {@link Status#OK} on success or {@link Status#NO_CONTENT} on failure.
   */
  @Path("/create/{activityName}/{role_uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("activityName") String activityName,
                         @PathParam("role_uuid") String role_uuid)
  {
    try
    {
      Activity activity = activityFactory.get(activityName);
      UUID uuid = UUID.fromString(role_uuid);
      Set<ActivityRoleAssociation> associations = associationFactory.associate(uuid, activity);
      Gson gson = new Gson();
      String jsonAssociations = gson.toJson(associations);
      return Response.ok(jsonAssociations).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NO_CONTENT).build();
    }
  }

  /**
   * Create an ActivityRoleAssociation for each Activity in an array
   * of activities linked to the provided UUID for a Role.
   * @param role_uuid The role id to assign the activities to
   * @param activities A list of activities to assign to the role id
   * @return {@link Status#OK} on success or {@link Status#NO_CONTENT} on failure.
   */
  @Path("/create/{role_uuid}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("role_uuid") String role_uuid, Activity[] activities)
  {
    try
    {
      UUID uuid = UUID.fromString(role_uuid);
      Set<ActivityRoleAssociation> associations = associationFactory.associate(uuid, activities);
      Gson gson = new Gson();
      String jsonAssociations = gson.toJson(associations);
      return Response.ok(jsonAssociations).build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NO_CONTENT).build();
    }
  }
  
  /**
   * Get all the ActiviteRoleAssociations for the given UUID of a Role.
   * @param role_uuid The role id to look up associated Activities.
   * @return {@link Status#OK} on success or {@link Status#NOT_FOUND} on failure.
   */
  @Path("/get/{role_uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("role_uuid") String role_uuid)
  {
    try
    {
      UUID uuid = UUID.fromString(role_uuid);
      List<ActivityRoleAssociation> associations = associationFactory.get(uuid);
      Gson gson = new Gson();
      String jsonAssociations = gson.toJson(associations);
      return Response.ok(jsonAssociations).build();
    }
    catch (NoResultException e){
      return Response.status(Status.NOT_FOUND).build();
    }
    catch(Exception e)
    {
      log.error("Error during retrieve role associations for \"{}\"", role_uuid, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Delete all of the ActivityRoleAssociations contained in the given list.
   * @param associations ActivityRoleAssociations to delete
   * @return {@link Status#OK} on success or {@link Status#NOT_MODIFIED} on failure.
   */
  @Path("/delete")
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  public Response delete(ActivityRoleAssociation[] associations)
  {
    try
    {
      associationFactory.delete(associations);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }

  /**
   * Delete all of the AcitivityRoleAssociations for a given Role identified by UUID.
   * @param role_uuid The role id to delete associated Activities.
   * @return {@link Status#OK} on success or {@link Status#NOT_MODIFIED} on failure.
   */
  @Path("/delete/{role_uuid}")
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  public Response delete(@PathParam("role_uuid") String role_uuid)
  {
    try
    {
      UUID uuid = UUID.fromString(role_uuid);
      associationFactory.delete(uuid);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }
}
