package io.ghap.provision.vpg;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Principal;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import io.ghap.provision.vpg.data.VPGFactory;
import io.ghap.provision.vpg.data.VirtualResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;

@Singleton
@Path("VirtualPrivateGrid")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators", "Data Curator", "Data Analyst"}, predicateType = PredicateType.OR)
public class VirtualPrivateGridResource
{
  private Logger logger = LoggerFactory.getLogger(VirtualPrivateGridResource.class);
  
  @Inject
  private VPGFactory vpgFactory;

  @Configuration("vpg.name")
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
   *  "templateUrl":"http://some/s3/location"
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

      UUID uuid = UUID.fromString(uuid_str);
      VirtualPrivateGrid vpg = vpgFactory.create(principal.getName(), uuid, activity);

      Gson gson = new Gson();
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
   * Get the stack for the provided user id.
   * @param uuid_str The unique user id
   * @return
   */
  @Path("/get/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      VirtualPrivateGrid vpg = vpgFactory.get(uuid);
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
   * @return
   */
  @Path("/status/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      String status = vpgFactory.status(uuid);
      Gson gson = new Gson();
      String jsonString = gson.toJson(status);
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
   * Get the pem file for a stack.  This is the file that
   * allows to login to the machine without providing a
   * password.
   * 
   * @param uuid_str The unique user id
   * @return
   */
  @Path("/pem/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public InputStream pem(@PathParam("uuid") String uuid_str)
  {
    InputStream inputStream = null;
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      VirtualPrivateGrid vpg = vpgFactory.get(uuid);
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
      String jsonString = gson.toJson(resources);
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
   * Check if a stack exists for the provided user id.
   * @param uuid_str The unique user id.
   * @return
   */
  @Path("/exists/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response exists(@PathParam("uuid") String uuid_str)
  {
    UUID uuid = UUID.fromString(uuid_str);
    if(vpgFactory.exists(uuid))
    {
      return Response.ok().build();
    }
    else
    {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * Destroy the stack for the provided user id.
   * @param uuid_str
   * @return
   */
  @Path("/terminate/{uuid}")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  public Response terminate(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      vpgFactory.delete(uuid);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_MODIFIED).entity(e.getMessage()).build();
    }
  }

  /**
   * Pauses a running stack for the provided user id.
   * @param uuid_str
   * @return
   */
  @Path("/pause/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response pause(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      vpgFactory.pause(uuid);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }
  /**
   * Resumes a stopped stack for the provided user id.
   * @param uuid_str
   * @return
   */
  @Path("/resume/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response resume(@PathParam("uuid") String uuid_str)
  {
    try
    {
      UUID uuid = UUID.fromString(uuid_str);
      vpgFactory.resume(uuid);
      return Response.ok().build();
    }
    catch(Exception e)
    {
      if(logger.isErrorEnabled())
      {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Status.NOT_MODIFIED).build();
    }
  }

  public String getServiceName()
  {
    return vpgName;
  }
}
