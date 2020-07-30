package io.ghap.project.manager;

import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.domain.PermissionRule;
import io.ghap.project.exception.ApplicationException;
import io.ghap.project.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author MaximTulupov@certara.com
 */
public interface ProjectService {

    Set<ApiProject> getAllProjects(UUID userId) throws ApplicationException;
    ApiProject createProject(SecurityContext securityContext, ApiCreateProject apiProject) throws ApplicationException;
    Set<ApiProject> getAllProjects() throws ApplicationException;

    void deleteProject(UUID id) throws ApplicationException;
    Set<ApiGrant> getAllGrants(SecurityContext securityContext, UUID userId, UUID projectId) throws ApplicationException;
    Set<ApiGrant> getAllGrants(SecurityContext securityContext, UUID projectId) throws ApplicationException;
    ApiGrant createGrant(SecurityContext securityContext, ApiCreateGrant apiGrant, UUID projectId) throws ApplicationException;
    void deleteGrant(UUID id) throws ApplicationException;
    ApiProject updateProject(ApiUpdateProject apiProject) throws ApplicationException;
    ApiGrant updateGrant(SecurityContext securityContext, ApiUpdateGrant apiGrant) throws ApplicationException;
    Set<LdapUser> getProjectUsers(SecurityContext securityContext, @PathParam("projectId") UUID projectId);
    Set<LdapUser> getGrantUsers(SecurityContext securityContext,@PathParam("grantId") UUID grantId);
    List<Commit> getHistoryByGrant(@PathParam("grantId") UUID grantId) throws StashException;
    void setUserManagementDao(UserManagementDao userManagementDao);
    DefaultResponse grantProjectPermissions(SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId, Set<PermissionRule> permissions);
    DefaultResponse revokeProjectPermissions(SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId, Set<PermissionRule> permissions);
    DefaultResponse grantGrantPermissions(SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId, Set<PermissionRule> permissions);
    DefaultResponse revokeGrantPermissions(SecurityContext securityContext, @PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId, Set<PermissionRule> permissions);
    Set<PermissionRule> getUserGrantPermissions(@PathParam("userId") UUID userId, @PathParam("grantId") UUID grantId);
    Set<PermissionRule> getUserProjectPermissions(@PathParam("userId") UUID userId, @PathParam("projectId") UUID projectId);
}
