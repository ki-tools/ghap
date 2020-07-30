package io.ghap.provision.vpg;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.provision.vpg.data.PersonalStorageFactory;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;

@Singleton
@Path("PersonalStorage")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class PersonalStorageResource
{
  private Logger logger = LoggerFactory.getLogger(PersonalStorageResource.class);
  
  @Inject
  private PersonalStorageFactory personalFactory;  

  @Configuration("personalstorage.name")
  private String personalStorageName;

  /**
   * Create personal user storage using EBS of the given size.
   * @param uuid_str The users UUID
   * @param g_size The size in gigabytes of the EBS volume to create.
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on error. 
   * The response will contain a JSON object that represents the users Personal Storage
   * <code>
   * {
   *  "guid":"77508b31-0ee1-4410-a282-12a4c0c86f7e",
   *  "uuid":"f3794efa-7f7f-403e-83f5-391ad217e27a",
   *  "g_size":1,
   *  "volume_id":"vol-98d2f3df",
   *  "availabilityZone":"us-east-1a"
   *  }
   * </code>
   */
  @Path("/create/{uuid}/{size}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@PathParam("uuid") String uuid_str, @PathParam("size") int g_size)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      PersonalStorage pStorage = personalFactory.create(uuid, g_size);
      Gson gson = new Gson();
      String jsonString = gson.toJson(pStorage);
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
   * Get a PersonalStorage object for a given user.  
   * @param uuid_str The users UUID
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on failure/error
   * The response will contain a JSON object that represents the users Personal Storage
   * <code>
   * {
   *  "guid":"77508b31-0ee1-4410-a282-12a4c0c86f7e",
   *  "uuid":"f3794efa-7f7f-403e-83f5-391ad217e27a",
   *  "g_size":1,
   *  "volume_id":"vol-98d2f3df",
   *  "availabilityZone":"us-east-1a"
   *  }
   * </code>
   */
  @Path("/get/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      PersonalStorage pStorage = personalFactory.get(uuid);
      Gson gson = new Gson();
      String jsonString = gson.toJson(pStorage);
      return Response.ok(jsonString).build();
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
   * Check if a user identified by a UUID has personal storage created for them
   * @param uuid_str The users UUID
   * @return {@link Status#OK} if the user has storage allocated or {@link Status#NOT_FOUND} if they do not.
   */
  @Path("/exists/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response exists(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      boolean exists = personalFactory.exists(uuid);
      return exists ? Response.ok().build() : Response.status(Status.NOT_FOUND).build();
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
   * Check if a user identified by a UUID has personal storage created for them
   * @param uuid_str The users UUID
   * @return {@link Status#OK} if the user storage is available {@link Status#NOT_FOUND} if it isn't.
   */
  @Path("/available/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response available(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      boolean exists = personalFactory.available(uuid);
      return exists ? Response.ok().build() : Response.status(Status.NOT_FOUND).build();
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
   * Check if a user identified by a UUID has personal storage created for them
   * @param uuid_str The users UUID
   * @return {@link Status#OK} with a message body containing a string of text representing the
   * state of the users personal storage.  {@link Status#NOT_FOUND} if the status cannot be determined
   */
  @Path("/state/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response state(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      String state = personalFactory.state(uuid);
      Gson gson = new Gson();
      String jsonString = gson.toJson(state);
      return Response.ok(jsonString).build();
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
   * Check if a user identified by a UUID has personal storage created for them
   * @param uuid_str The users UUID
   * @return {@link Status#OK} if the detach succeeds or {@link Status#NOT_FOUND} if it fails.
   */
  @Path("/detach/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response detach(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      personalFactory.detach(uuid);
      return Response.ok().build();
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
   * Delete the user storage for a user identified by a unique UUID
   * @param uuid_str The users UUID
   * @return {@link Status#OK} on success and {@link Status#NO_CONTENT} on faliure/error.
   */
  @Path("/delete/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      personalFactory.delete(uuid);
      return Response.ok().build();
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
  
  public String getServiceName() 
  {
    return personalStorageName;
  }
}
