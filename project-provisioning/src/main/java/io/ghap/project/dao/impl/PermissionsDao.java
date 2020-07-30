package io.ghap.project.dao.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.domain.*;
import io.ghap.project.model.LdapUser;
import io.ghap.project.model.StashException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.util.*;

/**
 */
@Singleton
public class PermissionsDao {

    private static final Logger log = LoggerFactory.getLogger(PermissionsDao.class);

    public static final Set<PermissionRule> FULL_PERMISSIONS = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);

    public static final int FULL_PERMISSIONS_INT = convertPermissionsToInt(FULL_PERMISSIONS);

    @Inject
    private CommonPersistDao commonPersistDao;

    @Inject
    private UserManagementDao userManagementDao;

    @Inject
    private StashProjectDao stashProjectDao;

    public List<Permission> loadPermissions(Project project, User user) {
        Map<String, Object> params = new HashMap<>();
        params.put("project", project);
        params.put("user", user);
        return commonPersistDao.executeQuery(Permission.class, "from Permission p inner join fetch p.project where p.project = :project and p.user = :user", params);
    }

    @Transactional
    public List<Permission> loadPermissions(Project project) {
        Map<String, Object> params = new HashMap<>();
        params.put("project", project);
        return commonPersistDao.executeQuery(Permission.class, "from Permission p  inner join fetch p.user where p.project = :project", params);
    }

    public List<Permission> loadPermissions(Repo repo, User user) {
        Map<String, Object> params = new HashMap<>();
        params.put("repo", repo);
        params.put("user", user);
        return commonPersistDao.executeQuery(Permission.class, "from Permission p inner join fetch p.repo re inner join fetch re.project where p.repo = :repo and p.user = :user", params);
    }

    @Transactional
    public List<Permission> loadPermissions(Repo repo) {
        Map<String, Object> params = new HashMap<>();
        params.put("repo", repo);
        return commonPersistDao.executeQuery(Permission.class, "from Permission p inner join fetch p.user where p.repo = :repo", params);
    }

    public void updatePermissions(List<Permission> permissions, Set<PermissionRule> rules) throws StashException {
        if (CollectionUtils.isEmpty(rules) || CollectionUtils.isEmpty(permissions)) {
            return;
        }
        for (Permission p : permissions) {
            p.setPermissions(convertPermissionsToInt(rules));
            stashProjectDao.updatePermission(p);
            commonPersistDao.update(p);
        }
    }

    public void revokePermissions(List<Permission> permissions, Set<PermissionRule> rules) throws StashException {
        if (CollectionUtils.isEmpty(rules) || CollectionUtils.isEmpty(permissions)) {
            return;
        }
        for (Permission p : permissions) {
            try {
                if (p.getPermissions() != null) {
					Set<PermissionRule> permissionRules = convertPermissionsToSet(p.getPermissions());
					permissionRules.removeAll(rules);
					p.setPermissions(convertPermissionsToInt(permissionRules));
					stashProjectDao.updatePermission(p);
					if (p.getPermissions() == 0) {
						commonPersistDao.delete(p);
					} else {
						commonPersistDao.update(p);
					}
				} else {
					commonPersistDao.delete(p);
				}
            } catch (RollbackException e) {
                handleError(p, "error change entity {} with id {}, messages = {}", e);
            }
        }
    }

    public Permission grant(User user, Project project, Set<PermissionRule> rules) throws StashException {
        if (CollectionUtils.isEmpty(rules)) {
            return null;
        }
        Permission p = new Permission();
        p.setUser(user);
        p.setProject(project);
        p.setPermissions(convertPermissionsToInt(rules));
        stashProjectDao.updatePermission(p);
        return commonPersistDao.create(p);
    }

    public Permission grant(User user, Repo repo, Set<PermissionRule> rules) throws StashException {
        if (CollectionUtils.isEmpty(rules)) {
            return null;
        }
        Permission p = new Permission();
        p.setUser(user);
        p.setRepo(repo);
        p.setPermissions(convertPermissionsToInt(rules));
        stashProjectDao.updatePermission(p);
        return commonPersistDao.create(p);
    }

    @Transactional
    public List<Project> loadProjectsAccordingToPermissions(User user, Set<PermissionRule> rules) {
        Integer permissions = convertPermissionsToInt(rules);
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("perm", permissions);
        return commonPersistDao.executeQuery(Project.class, "select p.project from Permission p where p.user = :user and p.permissions >= :perm", params);
    }

    @Transactional
    public List<Permission> loadProjectPermissionsAccordingToPermissions(User user, Set<PermissionRule> rules) {
        Integer permissions = convertPermissionsToInt(rules);
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("perm", permissions);
        return commonPersistDao.executeQuery(Permission.class, "select p from Permission p inner join fetch p.project pj where p.user = :user and p.permissions >= :perm", params);
    }

    @Transactional
    public List<Repo> loadReposAccordingToPermissions(User user, Set<PermissionRule> rules, Project project) {
        Integer permissions = convertPermissionsToInt(rules);
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("project", project);
        params.put("perm", permissions);
        return commonPersistDao.executeQuery(Repo.class, "select p.repo from Permission p join p.repo r where p.user = :user and p.permissions >= :perm and r.project = :project", params);
    }

    @Transactional
    public List<Permission> loadRepoPermissionsAccordingToPermissions(User user, Set<PermissionRule> rules, Project project) {
        Integer permissions = convertPermissionsToInt(rules);
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("project", project);
        params.put("perm", permissions);
        return commonPersistDao.executeQuery(Permission.class, "select p from Permission p inner join fetch p.repo r where p.user = :user and p.permissions >= :perm and r.project = :project", params);
    }

    @Transactional
    public List<Permission> loadPermissionsForProject(Project project) {
        Integer permissionsToInt = convertPermissionsToInt(Collections.singleton(PermissionRule.READ));
        Map<String, Object> params = new HashMap<>();
        params.put("project", project);
        params.put("perm", permissionsToInt);
        return commonPersistDao.executeQuery(Permission.class, "select p from Permission p inner join fetch p.user where p.project = :project and p.permissions >= :perm", params);
    }

    @Transactional
    public List<Permission> loadPermissionsForRepo(Repo repo) {
        Integer permissionsToInt = convertPermissionsToInt(Collections.singleton(PermissionRule.READ));
        Map<String, Object> params = new HashMap<>();
        params.put("repo", repo);
        params.put("perm", permissionsToInt);
        return commonPersistDao.executeQuery(Permission.class, "select p from Permission p inner join fetch p.user where p.repo = :repo and p.permissions >= :perm", params);
    }

    public void copyPermissions(List<Permission> projectPermissions, Repo repo) throws StashException {
        if (CollectionUtils.isEmpty(projectPermissions)) {
            return;
        }
        List<Permission> currentPermissions = loadPermissions(repo);
        for (Permission p : projectPermissions) {
            Permission currentPermission = null;
            for (Permission perm : currentPermissions) {
                if (perm.getUser().equals(p.getUser())) {
                    currentPermission = perm;
                    break;
                }
            }
            if (currentPermission != null) {
                currentPermission.setPermissions(Math.max(currentPermission.getPermissions(), p.getPermissions()));
                try {
                    stashProjectDao.updatePermission(currentPermission);
                    commonPersistDao.update(currentPermission);
                } catch (StashException e) {
                    log.error("error update permission in stash for user " + currentPermission.getUser().getLogin(), e);
                }
            } else {
                Permission permission = new Permission();
                permission.setRepo(repo);
                permission.setUser(p.getUser());
                permission.setPermissions(p.getPermissions());
                try {
                    stashProjectDao.updatePermission(permission);
                    commonPersistDao.create(permission);
                } catch (StashException e) {
                    log.error("error update permission in stash for user " + permission.getUser().getLogin(), e);
                }
            }
        }
    }

    public static Integer convertPermissionsToInt(Set attribute) {
        if (attribute == null) {
            return 0;
        }
        int value = 0;
        if (attribute.contains(PermissionRule.READ)) {
            value += 1;
        }
        if (attribute.contains(PermissionRule.WRITE)) {
            value += 2;
        }
        return value;
    }

    public static Set<PermissionRule> convertPermissionsToSet(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        EnumSet<PermissionRule> enumSet = EnumSet.of(PermissionRule.READ, PermissionRule.WRITE);
        if ((dbData & 1) == 0) {
            enumSet.remove(PermissionRule.READ);
        }
        if ((dbData & 2) == 0) {
            enumSet.remove(PermissionRule.WRITE);
        }
        return enumSet;
    }

    public void setupPermissions(Project project, Set<LdapUser> usersForGroup) throws StashException {
        for (LdapUser ldapUser : usersForGroup) {
            if (ldapUser.getGuid() == null || ldapUser.getEmail() == null || !ObjectClass.user.getName().equals(ldapUser.getObjectClass())) {
                log.info("{} not have guid or email or it is not user. Skip setup permissions", ldapUser);
                continue;
            }
            String userGuid = ldapUser.getGuid().toString();
            User user = commonPersistDao.getByExternalId(User.class, userGuid);
            boolean newUser = user == null;
            if (user == null) {
                user = new User(ObjectClass.fromString(ldapUser.getObjectClass()));
                user.setExternalId(userGuid);
                user.setEmail(ldapUser.getEmail());
                user.setLogin(ldapUser.getName());
                user = commonPersistDao.create(user);
            } else {
                user.setLogin(ldapUser.getName());
                commonPersistDao.update(user);
            }
            Permission permission = new Permission();
            permission.setProject(project);
            permission.setUser(user);
            permission.setPermissions(FULL_PERMISSIONS_INT);
            try {
                stashProjectDao.updatePermission(permission);
                commonPersistDao.create(permission);
            } catch (StashException e) {
                log.error("error set permission for user " + user.getLogin() + " for project " + project.getExternalId(), e);
                if (!newUser) {
                    log.error("try delete user");
                    try {
                        commonPersistDao.delete(user);
                    } catch (RollbackException e1) {
                        handleError(user, "error delete entity {} with id {}, messages = {}", e1);
                    }
                }
                continue;
            }
        }
    }

    public void setupPermissions(Repo repo, Set<LdapUser> usersForGroup) throws StashException {
        for (LdapUser ldapUser : usersForGroup) {
            if (ldapUser.getGuid() == null || ldapUser.getEmail() == null || !ObjectClass.user.getName().equals(ldapUser.getObjectClass())) {
                log.info("{} not have guid or email or it is not user. Skip setup permissions", ldapUser);
                continue;
            }
            String userGuid = ldapUser.getGuid().toString();
            User user = commonPersistDao.getByExternalId(User.class, userGuid);
            boolean newUser = user == null;
            if (user == null) {
                user = new User(ObjectClass.fromString(ldapUser.getObjectClass()));
                user.setExternalId(userGuid);
                user.setEmail(ldapUser.getEmail());
                user.setLogin(ldapUser.getName());
                user = commonPersistDao.create(user);
            }
            Permission permission = new Permission();
            permission.setRepo(repo);
            permission.setUser(user);
            permission.setPermissions(FULL_PERMISSIONS_INT);
            try {
                stashProjectDao.updatePermission(permission);
                commonPersistDao.create(permission);
            } catch (StashException e) {
                log.error("error set permission for user " + user.getLogin() + " for repo " + repo.getExternalId(), e);
                if (!newUser) {
                    log.error("try delete user");
                    try {
                        commonPersistDao.delete(user);
                    } catch (RollbackException e1) {
                        handleError(user, "error delete entity {} with id {}, messages = {}", e1);
                    }
                }
                continue;
            }
        }
    }
    public static  <T extends BaseDBEntity> void handleError(T t, String format, RollbackException e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            throw e;
        }
        if (!(cause instanceof OptimisticLockException)) {
            throw e;
        }
        log.error(format, t.getClass().getName(), t.getExternalId(), cause.getMessage());
    }
}
