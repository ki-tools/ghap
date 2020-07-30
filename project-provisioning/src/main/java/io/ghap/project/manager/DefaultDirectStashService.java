package io.ghap.project.manager;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.OAuthUser;
import io.ghap.oauth.OauthUtils;
import io.ghap.oauth.PredicateType;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.domain.*;
import io.ghap.project.exception.ApplicationException;
import io.ghap.project.model.*;
import io.ghap.project.model.Error;
import io.ghap.project.service.StashExceptionService;
import io.ghap.project.service.SyncDbWithGitService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

/**
 */
@Path("directStash")
@Singleton
@OAuth20
@AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator"}, predicateType = PredicateType.OR)
public class DefaultDirectStashService implements DirectStashService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @com.google.inject.Inject
    private StashProjectDao stashProjectDao;

    @com.google.inject.Inject
    private StashExceptionService stashExceptionService;

    @Inject
    private SyncDbWithGitService syncDbWithGitService;

    @Override
    @GET
    @Path("/get")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashProject> getProjects() {
        try {
            return stashProjectDao.getProjects();
        } catch (StashException e) {
            log.error("error getting projects from stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @GET
    @Path("/get/{projectKey}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashRepo> getRepos(@PathParam("projectKey") String projectKey) {
        if (StringUtils.isBlank(projectKey)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("project must not be null").build());
        }
        try {
            return stashProjectDao.getReposByProject(projectKey);
        } catch (StashException e) {
            log.error("error get list repos in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @GET
    @Path("/get/{projectKey}/permissions/{username}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashPermission> getUserPermissions(@PathParam("username") String username, @PathParam("projectKey") String projectKey) {
        return getStashPermissions(username, projectKey, true);
    }

    @Override
    @GET
    @Path("/get/{projectKey}/permissions")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashPermission> getUserPermissions(@PathParam("projectKey") String projectKey) {
        return getStashPermissions(null, projectKey, false);
    }

    @Override
    @GET
    @Path("/get/{projectKey}/permissions/{username}/repo/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashPermission> getUserPermissions(@PathParam("username") String username, @PathParam("projectKey") String projectKey, @PathParam("slug") String slug) {
        return getStashPermissions(username, projectKey, slug, true);
    }

    @Override
    @GET
    @Path("/get/{projectKey}/permissions/repo/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<StashPermission> getUserRepoPermissions(@PathParam("projectKey") String projectKey, @PathParam("slug") String slug) {
        return getStashPermissions(null, projectKey, slug, false);
    }

    @Override
    @GET
    @Path("/userPermissions/{projectKey}/{slug}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    public DefaultResponse isUserHasGrantPermission(@Context SecurityContext securityContext, @PathParam("projectKey") String projectKey, @PathParam("slug") String slug) {
        OAuthUser oAuthUser = OauthUtils.getOAuthUser(securityContext);
        if (oAuthUser == null) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        String name = oAuthUser.getName();
        if (StringUtils.isBlank(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        projectKey = projectKey.toUpperCase();
        Set<StashPermission> stashPermissions = getStashPermissions(name, projectKey, slug, false);
        if (CollectionUtils.isEmpty(stashPermissions)) {
            return new DefaultResponse(new HashSet<Error>());
        } else {
            return new DefaultResponse((Object)null);
        }
    }

    @Override
    @GET
    @Path("/userPermissions/{projectKey}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    public DefaultResponse isUserHasProjectPermission(@Context SecurityContext securityContext, @PathParam("projectKey") String projectKey) {
        OAuthUser oAuthUser = OauthUtils.getOAuthUser(securityContext);
        if (oAuthUser == null) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        String name = oAuthUser.getName();
        if (StringUtils.isBlank(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        projectKey = projectKey.toUpperCase();
        Set<StashPermission> stashPermissions = getStashPermissions(name, projectKey, false);
        if (CollectionUtils.isEmpty(stashPermissions)) {
            return new DefaultResponse(new HashSet<Error>());
        } else {
            return new DefaultResponse((Object)null);
        }
    }

    @Override
    @POST
    @Path("/fileExists")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    public Map<String, String> getFilePaths(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            log.error("file names required");
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("file names required").build());
        }
        try {
            return stashProjectDao.isFileExistsInStash(new HashSet<>(fileNames));
        } catch (StashException e) {
            log.error("error in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @GET
    @Path("/fileExists/{fileName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    public Map<String, String> filePath(@PathParam("fileName") String fileName) {
        if (StringUtils.isBlank(fileName)) {
            log.error("file name required");
            throw new WebApplicationException(HttpServletResponse.SC_BAD_REQUEST);
        }
        try {
            Map<String, String> fileExistsInStash = stashProjectDao.isFileExistsInStash(Collections.singleton(fileName));
            if (!fileExistsInStash.isEmpty()) {
                log.info("file {} exists in stash. return 200", fileName);
                return fileExistsInStash;
            } else {
                log.info("file {} does not exist in stash. return 404", fileName);
                throw new WebApplicationException(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (StashException e) {
            log.error("error in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/sync")
    @POST
    public DefaultResponse sync() {
        List<Error> errors = syncDbWithGitService.sync();
        if (errors.isEmpty()) {
            return DefaultResponse.SUCCESS;
        } else {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(new DefaultResponse(errors)).build());
        }
    }

    private Set<StashPermission> getStashPermissions(String username, String projectKey, String slug, boolean validateUsername) {
        if (StringUtils.isBlank(projectKey)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("project must not be null").build());
        }
        if (validateUsername && StringUtils.isBlank(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("username must not be null").build());
        }
        if (StringUtils.isBlank(slug)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("slug must not be null").build());
        }
        try {
            return stashProjectDao.getPermissionsByRepo(username, projectKey, slug);
        } catch (StashException e) {
            log.error("error get user permissions in stash", e);
            throw new ApplicationException(e.getCode(), stashExceptionService.toWebErrors(e));
        }
    }

    private Set<StashPermission> getStashPermissions(String username, String projectKey, boolean validateUsername) {
        if (StringUtils.isBlank(projectKey)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("project must not be null").build());
        }
        if (validateUsername && StringUtils.isBlank(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("username must not be null").build());
        }
        try {
            return stashProjectDao.getPermissionsByProject(username, projectKey);
        } catch (StashException e) {
            log.error("error get user permissions in stash", e);
            throw new ApplicationException(e.getCode(), stashExceptionService.toWebErrors(e));
        }
    }
}
