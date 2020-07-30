package io.ghap.project.manager;

import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.project.domain.StashPermission;
import io.ghap.project.domain.StashProject;
import io.ghap.project.domain.StashRepo;
import io.ghap.project.model.DefaultResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public interface DirectStashService {
    @GET
    @Path("/get")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashProject> getProjects();

    @GET
    @Path("/get/{projectKey}")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashRepo> getRepos(@PathParam("projectKey") String projectKey);

    @GET
    @Path("/get/{projectKey}/permissions/{username}")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashPermission> getUserPermissions(@PathParam("username") String username, @PathParam("projectKey") String projectKey);

    @GET
    @Path("/get/{projectKey}/permissions")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashPermission> getUserPermissions(@PathParam("projectKey") String projectKey);

    @GET
    @Path("/get/{projectKey}/permissions/{username}/repo/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashPermission> getUserPermissions(@PathParam("username") String username, @PathParam("projectKey") String projectKey, @PathParam("slug") String slug);

    @GET
    @Path("/get/{projectKey}/permissions/repo/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    Set<StashPermission> getUserRepoPermissions(@PathParam("projectKey") String projectKey, @PathParam("slug") String slug);

    @GET
    @Path("/userPermissions/{projectKey}/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    DefaultResponse isUserHasGrantPermission(@Context SecurityContext securityContext, @PathParam("projectKey") String projectKey, @PathParam("slug") String slug);

    @GET
    @Path("/userPermissions/{projectKey}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    DefaultResponse isUserHasProjectPermission(@Context SecurityContext securityContext, @PathParam("projectKey") String projectKey);

    @POST
    @Path("/fileExists")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    Map<String, String> getFilePaths(List<String> fileNames);

    @GET
    @Path("/fileExists/{fileName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    Map<String, String> filePath(@PathParam("fileName") String fileName);

    @Consumes("application/json")
    @Produces("application/json")
    @Path("/sync")
    @POST
    DefaultResponse sync();
}
