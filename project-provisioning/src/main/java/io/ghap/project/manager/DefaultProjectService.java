package io.ghap.project.manager;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.OAuthUser;
import io.ghap.oauth.OauthUtils;
import io.ghap.oauth.PredicateType;
import io.ghap.project.dao.CleanupDao;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.dao.impl.PermissionsDao;
import io.ghap.project.domain.*;
import io.ghap.project.exception.ApplicationException;
import io.ghap.project.model.*;
import io.ghap.project.model.Error;
import io.ghap.project.service.StashExceptionService;
import io.ghap.utils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author MaximTulupov@certara.com
 */
@Path("project")
@Singleton
@OAuth20
@AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Curator"}, predicateType = PredicateType.OR)
public class DefaultProjectService implements ProjectService {

    private static final List<String> EXCLUDES = Collections.singletonList("id");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @com.google.inject.Inject
    private StashProjectDao stashProjectDao;

    @com.google.inject.Inject
    private CommonPersistDao commonPersistDao;

    @com.google.inject.Inject
    private StashExceptionService stashExceptionService;

    @Inject
    private PermissionsDao permissionsDao;

    @Inject
    private UserManagementDao userManagementDao;


    @Inject
    public DefaultProjectService(CleanupDao cleanupDao) {
        cleanupDao.scheduleCleanupProcedure();
    }

    @Override
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ApiProject createProject(@Context SecurityContext securityContext, @Valid ApiCreateProject apiProject) throws ApplicationException {
        log.info("start create project {}", apiProject.getKey());
        StashProject project = (StashProject) BeanUtils.copyProperties(apiProject, new StashProject(), EXCLUDES);
        //Important. DO not allow set key different that name. In UI we specify only name
        project.setKey(project.getName());

        //create project in stash
        Project dbProject = new Project();
        try {
            try {
                project = stashProjectDao.createProject(project);
            } catch (StashException e) {
                //ignore validation error during create. Just create repo in our DB.
                if (e.getCode() != HttpServletResponse.SC_CONFLICT) {
                    throw e;
                }
                //project exists in our db. throw stash error
                if (isProjectExists(project.getKey())) {
                    throw e;
                }
            }
            //save project in the DB
            dbProject.setExternalId(project.getKey());
            commonPersistDao.create(dbProject);

            //load users to grant permissions
            Set<LdapUser> usersForGroup = userManagementDao.getUsersForGroup(OauthUtils.getAccessToken(securityContext));

            //setup permissions in db
            permissionsDao.setupPermissions(dbProject, usersForGroup);
            ApiProject result = (ApiProject) BeanUtils.copyProperties(project, new ApiCreateProject(), EXCLUDES);
            result.setId(dbProject.getId());
            return result;
        } catch (StashException e) {
            log.error("error create project in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        } catch (Throwable e) {
            log.error("error create project ", e);
            try {
                deleteProject(dbProject.getId());
            } catch (Throwable e1) {
                log.error("error delete project", e1);
            }
            throw e;
        }
    }

    @Override
    @GET
    @Path("/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public Set<ApiProject> getAllProjects(@PathParam("userId") UUID userId) throws ApplicationException {
        log.info("get allprojects for {}", userId);
        Set<StashProject> projects = null;
        try {
            projects = stashProjectDao.getProjects();
        } catch (StashException e) {
            log.error("error getting projects from stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
        if (userId == null) {
            return convertProjects(projects);
        }
        User user = commonPersistDao.getByExternalId(User.class, userId.toString());
        validateExists(user, null);
        filterProjects(projects, EnumSet.of(PermissionRule.READ), user);
        return convertProjects(projects);
    }

    @Override
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public Set<ApiProject> getAllProjects() throws ApplicationException {
        return getAllProjects(null);
    }

    @Override
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public ApiProject updateProject(@Valid ApiUpdateProject apiProject) throws ApplicationException {
        log.info("start update project {}", apiProject.getKey());
        StashProject stashProject = (StashProject) BeanUtils.copyProperties(apiProject, new StashProject(), EXCLUDES);
        Project project = commonPersistDao.read(Project.class, apiProject.getId());
        validateExists(project, null);

        //check if client send empty values
        if (StringUtils.isEmpty(apiProject.getDescription()) && StringUtils.isBlank(apiProject.getName())) {
            apiProject.setKey(null);
            apiProject.setId(project.getId());
            return apiProject;
        }
        stashProject.setKey(project.getExternalId());
        try {
            stashProject = stashProjectDao.updateProject(stashProject);
            apiProject = (ApiUpdateProject) BeanUtils.copyProperties(stashProject, new ApiUpdateProject(), EXCLUDES);
            apiProject.setId(project.getId());
            return apiProject;
        } catch (StashException e) {
            log.error("error create project in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public void deleteProject(@PathParam("id") UUID id) throws ApplicationException {
        log.info("start delete project {}", id);
        Project project = commonPersistDao.read(Project.class, id);
        validateExists(project, null);
        try {
            log.info("start delete project {}, {}", project.getExternalId(), project.getId());
            commonPersistDao.delete(project);
            String stashId = project.getExternalId();
            for (StashRepo stashRepo : stashProjectDao.getReposByProject(stashId)) {
                try {
                    stashProjectDao.deleteRepo(stashId, stashRepo.getSlug());
                } catch (StashException e) {
                    if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                        //repo do not exists in stash. skip error
                        continue;
                    }
                }
            }
            try {
                stashProjectDao.deleteProject(stashId);
            } catch (StashException e) {
                if (e.getCode() != HttpServletResponse.SC_NOT_FOUND) {
                    throw e;
                }
            }

        } catch (StashException e) {
            log.error("error delete project in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        } catch (RollbackException e1) {
            PermissionsDao.handleError(project, "error delete entity {} with id {}, messages = {}", e1);
        }
    }

    @Override
    @GET
    @Path("/{projectId}/grants/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public Set<ApiGrant> getAllGrants(@Context SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId) throws ApplicationException {
        log.info("start get all grants  project id = {} user id =  {}", projectId, userId);
        Project project = commonPersistDao.read(Project.class, projectId);
        validateExists(project, null);
        Set<StashRepo> repoSet = null;
        try {
            repoSet = stashProjectDao.getReposByProject(project.getExternalId());
        } catch (StashException e) {
            log.error("error get list repos in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
        if (userId == null) {
            OAuthUser user = OauthUtils.getOAuthUser(securityContext);
            return convertRepos(repoSet, project, user == null ? null : user.getName());
        }
        User user = commonPersistDao.getByExternalId(User.class, userId.toString());
        validateExists(user, null);
        filterRepos(repoSet, EnumSet.of(PermissionRule.READ), user, project);
        return convertRepos(repoSet, project, user.getLogin());
    }

    @Override
    @GET
    @Path("/{projectId}/grants")
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public Set<ApiGrant> getAllGrants(@Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) throws ApplicationException {
        return getAllGrants(securityContext, null, projectId);
    }

    @Override
    @POST
    @Path("/{projectId}/grant")
    @Consumes("application/json")
    @Produces("application/json")
    public ApiGrant createGrant(@Context SecurityContext securityContext, @Valid ApiCreateGrant apiGrant, @PathParam("projectId") UUID projectId) throws ApplicationException {
        log.info("start create grant {}", apiGrant.getName());
        StashRepo stashRepo = (StashRepo) BeanUtils.copyProperties(apiGrant, new StashRepo(), EXCLUDES);
        stashRepo.setSlug(apiGrant.getName());
        Project project = commonPersistDao.read(Project.class, projectId);
        validateExists(project, null);
        Repo dbRepo = new Repo();
        try {
            try {
                stashRepo = stashProjectDao.createRepo(project.getExternalId(), stashRepo);
            } catch (StashException e) {
                //ignore validation error during create. Just create repo in our DB.
                if (e.getCode() != HttpServletResponse.SC_CONFLICT) {
                    throw e;
                }
                //repo exists in our db. throw stash error
                if (isGrantExists(project, stashRepo.getSlug())) {
                    throw e;
                }
            }


            dbRepo.setExternalId(stashRepo.getSlug());
            dbRepo.setProject(project);
            dbRepo = commonPersistDao.create(dbRepo);

            Set<LdapUser> usersForGroup = userManagementDao.getUsersForGroup(OauthUtils.getAccessToken(securityContext));
            permissionsDao.setupPermissions(dbRepo, usersForGroup);
            List<Permission> permissions = permissionsDao.loadPermissions(project);
            permissionsDao.copyPermissions(permissions, dbRepo);
            ApiGrant result = (ApiGrant) BeanUtils.copyProperties(stashRepo, new ApiCreateGrant(), EXCLUDES);
            result.setId(dbRepo.getId());
            OAuthUser user = OauthUtils.getOAuthUser(securityContext);
            if (user != null) {
                updateCloneUrl(apiGrant, user.getName());
            }
            return result;
        } catch (StashException e) {
            log.error("error create repo in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        } catch (Throwable e) {
            log.error("error create repo ", e);
            try {
                deleteGrant(dbRepo.getId());
            } catch (Throwable e1) {
                log.error("error create repo", e1);
            }
            throw e;
        }
    }

    @Override
    @PUT
    @Path("/grant")
    @Consumes("application/json")
    @Produces("application/json")
    public ApiGrant updateGrant(@Context SecurityContext securityContext, @Valid ApiUpdateGrant apiGrant) throws ApplicationException {
        log.info("start update grant {}", apiGrant.getName());
        StashRepo stashRepo = (StashRepo) BeanUtils.copyProperties(apiGrant, new StashRepo());
        stashRepo.setSlug(apiGrant.getName());
        Repo repo = commonPersistDao.read(Repo.class, apiGrant.getId());
        validateExists(repo, null);
        Project project = repo.getProject();
        validateExists(project, null);
        try {
            stashRepo.setSlug(repo.getExternalId());
            stashRepo = stashProjectDao.updateRepo(project.getExternalId(), stashRepo);
            repo.setExternalId(stashRepo.getSlug());
            repo = commonPersistDao.update(repo);

            apiGrant = (ApiUpdateGrant) BeanUtils.copyProperties(stashRepo, new ApiUpdateGrant(), EXCLUDES);
            apiGrant.setId(repo.getId());
            OAuthUser user = OauthUtils.getOAuthUser(securityContext);
            if (user != null) {
                updateCloneUrl(apiGrant, user.getName());
            }
            return apiGrant;
        } catch (StashException e) {
            log.error("error update repo in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }
    }

    @Override
    @DELETE
    @Path("/grant/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public void deleteGrant(@PathParam("id") UUID id) throws ApplicationException {
        log.info("start delete grant {}", id);
        Repo repo = commonPersistDao.read(Repo.class, id);
        validateExists(repo, null);
        Project project = repo.getProject();
        validateExists(project, null);
        try {
            try {
                stashProjectDao.deleteRepo(project.getExternalId(), repo.getExternalId());
            } catch (StashException e) {
                if (e.getCode() != HttpServletResponse.SC_NOT_FOUND) {
                    throw e;
                }
            }
            log.info("start delete repo {}, {}", repo.getExternalId(), repo.getId());
            commonPersistDao.delete(repo);
        } catch (StashException e) {
            log.error("error delete repo in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        } catch (RollbackException e1) {
            PermissionsDao.handleError(repo, "error delete entity {} with id {}, messages = {}", e1);
        }
    }

    @Override
    @POST
    @Path("/{projectId}/grantProjectPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public DefaultResponse grantProjectPermissions(@Context SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId, Set<PermissionRule> permissions) {
        log.info("start grant project permissions project id = {} user id = ", projectId, userId);
        Project project = commonPersistDao.read(Project.class, projectId);
        validateExists(project, null);

        User user = loadUser(securityContext, userId);

        List<Permission> permissionList = permissionsDao.loadPermissions(project, user);
        try {
            try {
                if (CollectionUtils.isEmpty(permissionList)) {
                    permissionsDao.grant(user, project, permissions);
                } else {
                    permissionsDao.updatePermissions(permissionList, permissions);
                }
            } catch (StashException e) {
                handleStashProjectError(project, e);
            }

            //grant permissions from repos
            Map<String, Object> params = new HashMap<>();
            params.put("project", project);
            List<Repo> repos = commonPersistDao.executeQuery(Repo.class, "select r from Repo r where r.project = :project", params);
            boolean grantWasDeleted = false;
            for (Repo repo : repos) {
                List<Permission> repoPermissions = permissionsDao.loadPermissions(repo, user);
                try {
                    if (CollectionUtils.isEmpty(repoPermissions)) {
                        permissionsDao.grant(user, repo, permissions);
                    } else {
                        permissionsDao.updatePermissions(repoPermissions, permissions);
                    }
                } catch (StashException e) {
                    grantWasDeleted = handleStashGrantError(repo, e);
                }
            }
            //TODO throw error if grant was delete
//            if (grantWasDeleted) {
//                throw new WebApplicationException(HttpServletResponse.SC_GONE);
//            }

        } catch (StashException e) {
            log.error("error update permissions in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }

        return new DefaultResponse((Object) null);
    }

    @Override
    @POST
    @Path("/{projectId}/revokeProjectPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public DefaultResponse revokeProjectPermissions(@Context SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId, Set<PermissionRule> permissions) {
        log.info("start revoke project permissions project id = {} user id = ", projectId, userId);
        User user = loadUser(securityContext, userId);

        Project project = commonPersistDao.read(Project.class, projectId);
        if (project == null) {
            return new DefaultResponse(new Error(404, "Cannot find project with ID: " + projectId));
        }

        List<Permission> permissionList = permissionsDao.loadPermissions(project, user);
        if (!CollectionUtils.isEmpty(permissionList)) {
            try {
                try {
                    permissionsDao.revokePermissions(permissionList, permissions);
                } catch (StashException e) {
                    handleStashProjectError(project, e);
                }

                //remove permissions from repos
                Map<String, Object> params = new HashMap<>();
                params.put("project", project);
                List<Repo> repos = commonPersistDao.executeQuery(Repo.class, "select r from Repo r where r.project = :project", params);
                boolean grantWasDeleted = false;
                for (Repo repo : repos) {
                    List<Permission> repoPermissions = permissionsDao.loadPermissions(repo, user);
                    if (!CollectionUtils.isEmpty(repoPermissions)) {
                        try {
                            permissionsDao.revokePermissions(repoPermissions, permissions);
                        } catch (StashException e) {
                            grantWasDeleted = handleStashGrantError(repo, e);
                        }
                    }
                }
                //TODO throw this error if we need reread data.
//                if (grantWasDeleted) {
//                    throw new WebApplicationException(HttpServletResponse.SC_GONE);
//                }
            } catch (StashException e) {
                log.error("error create repo in stash", e);
                throw new ApplicationException(stashExceptionService.toWebErrors(e));
            }
        }
        return new DefaultResponse((Object) null);
    }

    @Override
    @POST
    @Path("/{grantId}/grantGrantPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public DefaultResponse grantGrantPermissions(@Context SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId, Set<PermissionRule> permissions) {
        log.info("start grant grant permissions grant id = {} user id = ", grantId, userId);
        Repo repo = commonPersistDao.read(Repo.class, grantId);
        validateExists(repo, "Cannot find repo with ID: " + grantId);

        User user = loadUser(securityContext, userId);

        List<Permission> permissionList = permissionsDao.loadPermissions(repo, user);
        try {
            try {
                if (CollectionUtils.isEmpty(permissionList)) {
                    permissionsDao.grant(user, repo, permissions);
                } else {
                    permissionsDao.updatePermissions(permissionList, permissions);
                }
            } catch (StashException e) {
                handleStashGrantError(repo, e);
            }
        } catch (StashException e) {
            log.error("error update permissions in stash", e);
            throw new ApplicationException(stashExceptionService.toWebErrors(e));
        }

        return new DefaultResponse((Object) null);
    }

    @Override
    @POST
    @Path("/{grantId}/revokeGrantPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public DefaultResponse revokeGrantPermissions(@Context SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId, Set<PermissionRule> permissions) {
        log.info("start revoke grant permissions grant id = {} user id = ", grantId, userId);
        User user = loadUser(securityContext, userId);

        Repo repo = commonPersistDao.read(Repo.class, grantId);
        if (repo == null) {
            return new DefaultResponse(new Error(404, "Cannot find repo with ID: " + grantId));
        }

        List<Permission> permissionList = permissionsDao.loadPermissions(repo, user);
        if (!CollectionUtils.isEmpty(permissionList)) {
            try {
                try {
                    permissionsDao.revokePermissions(permissionList, permissions);
                } catch (StashException e) {
                    handleStashGrantError(repo, e);
                }
            } catch (StashException e) {
                log.error("error revoke grant permissions in stash", e);
                throw new ApplicationException(stashExceptionService.toWebErrors(e));
            }
        }

        return new
                DefaultResponse((Object) null);
    }

    @Override
    @GET
    @Path("/{grantId}/getGrantPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<PermissionRule> getUserGrantPermissions(@PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId) {
        log.info("get user permissions for grant grant id = {} user id = {}", grantId, userId);
        User user = commonPersistDao.getByExternalId(User.class, userId.toString());
        validateExists(user, null);
        Repo repo = commonPersistDao.read(Repo.class, grantId);
        validateExists(repo, null);
        Set<PermissionRule> permissionRules = new HashSet<>();
        for (Permission permission : permissionsDao.loadPermissions(repo, user)) {
            Set<PermissionRule> permissionsByRepo = permissionsDao.convertPermissionsToSet(permission.getPermissions());
            if (permissionsByRepo != null) {
                permissionRules.addAll(permissionsByRepo);
            }
        }
        return permissionRules;
    }

    @Override
    @GET
    @Path("/{projectId}/getProjectPermissions/{userId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<PermissionRule> getUserProjectPermissions(@PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId) {
        log.info("get user permissions for project project id = {} user id = {}", projectId, userId);
        User user = commonPersistDao.getByExternalId(User.class, userId.toString());
        validateExists(user, null);
        Project project = commonPersistDao.read(Project.class, projectId);
        validateExists(project, null);
        Set<PermissionRule> permissionRules = new HashSet<>();
        for (Permission permission : permissionsDao.loadPermissions(project, user)) {
            Set<PermissionRule> permissionsByProject = permissionsDao.convertPermissionsToSet(permission.getPermissions());
            if (permissionsByProject != null) {
                permissionRules.addAll(permissionsByProject);
            }
        }
        return permissionRules;
    }

    @Override
    @GET
    @Path("/{projectId}/users")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<LdapUser> getProjectUsers(@Context SecurityContext securityContext, @PathParam("projectId") UUID projectId) {
        log.info("start get users for project {}", projectId);
        Project project = commonPersistDao.read(Project.class, projectId);
        validateExists(project, null);
        List<Permission> permissions = permissionsDao.loadPermissionsForProject(project);
        return convertUsers(permissions, OauthUtils.getAccessToken(securityContext));
    }

    @Override
    @GET
    @Path("/grant/{grantId}/users")
    @Consumes("application/json")
    @Produces("application/json")
    public Set<LdapUser> getGrantUsers(@Context SecurityContext securityContext, @PathParam("grantId") UUID grantId) {
        log.info("start get users for grant {}", grantId);
        Repo repo = commonPersistDao.read(Repo.class, grantId);
        validateExists(repo, null);
        List<Permission> permissions = permissionsDao.loadPermissionsForRepo(repo);
        return convertUsers(permissions, OauthUtils.getAccessToken(securityContext));
    }

    @Override
    @GET
    @Path("/grant/{grantId}/history")
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer"}, predicateType = PredicateType.OR)
    public List<Commit> getHistoryByGrant(@PathParam("grantId") UUID grantId) throws StashException {
        log.info("start get grant history {}", grantId);
        Repo repo = commonPersistDao.read(Repo.class, grantId);
        validateExists(repo, null);
        List<Commit> commits= stashProjectDao.getHistoryByRepo(repo.getProject().getExternalId(), repo.getExternalId());
        return fillCommitDatesAndSort(commits);
    }

    private List<Commit> fillCommitDatesAndSort(List<Commit> commits) {
        SimpleDateFormat commitDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Commit commit : commits) {
            commit.setCommitDate(commitDateFormat.format(new Date(commit.getAuthorTimestamp())));
        }
        Collections.sort(commits, new Comparator<Commit>() {
            @Override
            public int compare(Commit o1, Commit o2) {
                return new Date(o2.getAuthorTimestamp()).compareTo(new Date(o1.getAuthorTimestamp()));
            }
        });
        return commits;
    }

    @Override
    public void setUserManagementDao(UserManagementDao userManagementDao) {
        this.userManagementDao = userManagementDao;
    }

    private Set<ApiProject> convertProjects(Set<StashProject> stashProjects) {
        if (CollectionUtils.isEmpty(stashProjects)) {
            return new HashSet<>();
        }
        Set<String> ids = new HashSet<>(stashProjects.size());
        for (StashProject p : stashProjects) {
            ids.add(p.getKey().toLowerCase());
        }
        List<Project> projects = commonPersistDao.getByExternalIdCI(Project.class, ids);

        Set<ApiProject> apiProjects = new TreeSet<ApiProject>(new Comparator<ApiProject>() {
            @Override
            public int compare(ApiProject o1, ApiProject o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });
        for (StashProject p : stashProjects) {
            for (Project project : projects) {
                if (project.getExternalId().equalsIgnoreCase(p.getKey())) {
                    ApiProject apiProject = (ApiProject) BeanUtils.copyProperties(p, new ApiUpdateProject(), EXCLUDES);
                    apiProject.setId(project.getId());
                    apiProjects.add(apiProject);
                    break;
                }
            }
        }
        return apiProjects;
    }

    private Set<ApiGrant> convertRepos(Set<StashRepo> stashRepos, Project project, String login) {
        if (CollectionUtils.isEmpty(stashRepos)) {
            return new HashSet<>();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("project", project);
        List<Repo> repos = commonPersistDao.executeQuery(Repo.class, "select r from Repo r where r.project = :project", params);

        Set<ApiGrant> apiGrants = new TreeSet<ApiGrant>(new Comparator<ApiGrant>() {
            @Override
            public int compare(ApiGrant o1, ApiGrant o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });
        for (StashRepo p : stashRepos) {
            for (Repo repo : repos) {
                if (repo.getExternalId().equalsIgnoreCase(p.getSlug())) {
                    ApiGrant apiGrant = (ApiGrant) BeanUtils.copyProperties(p, new ApiCreateGrant(), EXCLUDES);
                    apiGrant.setId(repo.getId());
                    apiGrants.add(apiGrant);
                    updateCloneUrl(apiGrant, login);
                    break;
                }
            }
        }
        return apiGrants;
    }

    private void validateExists(Object entity, String message) {
        if (entity == null) {
            throw (message == null) ? new NotFoundException():new NotFoundException(message);
        }
    }

    private void filterProjects(Set<StashProject> projects, Set<PermissionRule> rules, User user) {
        List<Permission> list = permissionsDao.loadProjectPermissionsAccordingToPermissions(user, rules);
        if (CollectionUtils.isEmpty(list)) {
            projects.clear();
            return;
        }
        for (Iterator<StashProject> it = projects.iterator(); it.hasNext(); ) {
            boolean found = false;
            StashProject stashProject = it.next();
            for (Permission permission : list) {
                if (permission.getProject() == null) {
                    continue;
                }
                if (stashProject.getKey().equalsIgnoreCase(permission.getProject().getExternalId())) {
                    found = true;
                    stashProject.setPermissions(PermissionsDao.convertPermissionsToSet(permission.getPermissions()));
                    break;
                }
            }
            if (!found) {
                it.remove();
            }
        }
    }

    private void filterRepos(Set<StashRepo> stashRepos, Set<PermissionRule> rules, User user, Project project) {
        List<Permission> list = permissionsDao.loadRepoPermissionsAccordingToPermissions(user, rules, project);
        if (CollectionUtils.isEmpty(list)) {
            stashRepos.clear();
            return;
        }
        for (Iterator<StashRepo> it = stashRepos.iterator(); it.hasNext(); ) {
            boolean found = false;
            StashRepo stashRepo = it.next();
            for (Permission permission : list) {
                if (permission.getRepo() == null) {
                    continue;
                }
                if (stashRepo.getSlug().equalsIgnoreCase(permission.getRepo().getExternalId())) {
                    found = true;
                    stashRepo.setPermissions(PermissionsDao.convertPermissionsToSet(permission.getPermissions()));
                    break;
                }
            }
            if (!found) {
                it.remove();
            }
        }
    }

    private User createUser(LdapUser ldapUser) {
        User user = new User(ObjectClass.fromString(ldapUser.getObjectClass()));
        user.setEmail(ldapUser.getEmail());
        user.setExternalId(ldapUser.getGuid().toString());
        user.setLogin(ldapUser.getName());
        return commonPersistDao.create(user);
    }

    private User updateUser(User user, LdapUser ldapUser) {
        user.setEmail(ldapUser.getEmail());
        user.setExternalId(ldapUser.getGuid().toString());
        user.setLogin(ldapUser.getName());
        return commonPersistDao.update(user);
    }

    private Set<LdapUser> convertUsers(List<Permission> permissions, String accessToken) {
        if (CollectionUtils.isEmpty(permissions)) {
            return Collections.emptySet();
        }
        Set<LdapUser> result = new HashSet<>(permissions.size());
        for (Permission p : permissions) {
            if (p.getUser() == null) {
                continue;
            }
            try {
                LdapUser user = new LdapUser();
                user.setGuid(UUID.fromString(p.getUser().getExternalId()));
                user.setPermissions(PermissionsDao.convertPermissionsToSet(p.getPermissions()));
                result.add(user);
            } catch (Exception e) {
                log.error("error convert user " + p.getExternalId(), e);
            }
        }
        return result;
    }

    private void updateCloneUrl(ApiGrant grant, String login) {
        if (grant == null || StringUtils.isBlank(grant.getCloneUrl()) || StringUtils.isBlank(login)) {
            return;
        }
        String cloneUrl = grant.getCloneUrl();
        grant.setCloneUrl(cloneUrl.replace(stashProjectDao.getLogin(), login));
    }

    private boolean handleStashGrantError(Repo repo, StashException stashException) throws StashException {
        if (stashException.getCode() != HttpServletResponse.SC_NOT_FOUND) {
            throw stashException;
        }

        log.error("Inconsistency between DB and Stash was found(repo ID: " + repo.getId() + ").", stashException);

        boolean grantWasDeleted = false;

        Project project = repo.getProject();
        validateExists(project, null);


        // delete grant from DB if it doesn't exist in a Stash
        try {
            boolean needToDelete = false;
            try {
                StashRepo stashRepo = stashProjectDao.getRepo(project.getExternalId(), repo.getExternalId());
            } catch (StashException e) {
                if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                    needToDelete = true;
                }
                else {
                    log.error("error sync grant with guid " + repo.getId() + ". Error: " + e, e);
                }
            }

            if(needToDelete){
                log.info("start delete(sync) repo {}, {}", repo.getExternalId(), repo.getId());
                commonPersistDao.delete(repo);
                grantWasDeleted = true;
            }

        } catch (RollbackException e1) {
            PermissionsDao.handleError(repo, "error delete entity {} with id {}, messages = {}", e1);
        }


        return grantWasDeleted;
    }

    private void handleStashProjectError(Project project, StashException stashException) throws StashException {
        if (stashException.getCode() != HttpServletResponse.SC_NOT_FOUND) {
            throw stashException;
        }
        log.error("Inconsistency between DB and Stash was found(project ID: " + project.getId() + ").", stashException);
        //we get 404 error. This means project was deleted from stash.

        String stashId = project.getExternalId();
        boolean needToDelete = false;
        try{
            stashProjectDao.getProject(project.getExternalId());
        } catch (StashException e){
            if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                // delete project from DB since it doesn't exist in Stash
                needToDelete = true;
            }
            else {
                log.error("error sync project with guid " + project.getId() + ". Error: " + e, e);
            }
        }

        if(needToDelete) try {
            log.info("start delete(sync) project {}, {}", project.getExternalId(), project.getId());
            commonPersistDao.delete(project);
        } catch (RollbackException e1) {
            PermissionsDao.handleError(project, "error sync entity {} with id {}, messages = {}", e1);
        }

        // TODO If we need reload data throw this error
//        throw new WebApplicationException(HttpServletResponse.SC_GONE);
    }

    private boolean isGrantExists(Project project, String slug) {
        Map<String, Object> params = new HashMap<>();
        params.put("project", project);
        params.put("id", slug);
        List<Repo> repos = commonPersistDao.executeQuery(Repo.class, "select e from Repo e where e.project = :project and LOWER(e.externalId) = LOWER(:id)", params);
        return repos != null && !repos.isEmpty();
    }

    private boolean isProjectExists(String name) {
        Project project = commonPersistDao.getByExternalIdCI(Project.class, name);
        return project != null;
    }

    private User loadUser(SecurityContext securityContext, UUID userId){
        LdapUser ldapUser = userManagementDao.getUser(userId, OauthUtils.getAccessToken(securityContext));
        User user = commonPersistDao.getByExternalId(User.class, userId.toString());
        if (user == null) {
            validateExists(ldapUser, "Cannot find LDAP user with ID: " + userId);
            try {
                user = createUser(ldapUser);
            } catch (PersistenceException e) {
                user = commonPersistDao.getByExternalId(User.class, userId.toString());
                if(user == null){
                    throw new NotFoundException("Cannot find user with ID: " + userId);
                }
            }
        }
        else if((!user.isGroup() && (!ObjectUtils.equals(user.getLogin(), ldapUser.getName()) || !ObjectUtils.equals(user.getEmail(), ldapUser.getEmail()))
                || (user.isGroup() && !ObjectUtils.equals(user.getLogin(), ldapUser.getName()))) ){
            try {
                user = updateUser(user, ldapUser);
            } catch (PersistenceException e) {
                //ignore
            }
        }
        return user;
    }
}
