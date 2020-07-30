package io.ghap.project.manager;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Singleton;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.dao.impl.PermissionsDao;
import io.ghap.project.domain.*;
import io.ghap.project.model.StashException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("report")
@Singleton
@OAuth20
@AllowedScopes(scopes = { "GhapAdministrators", "Administrators", "GHAP Administrator" }, predicateType = PredicateType.OR)
public class DefaultReportService {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  private CommonPersistDao commonDao;

  @Inject
  private StashProjectDao stashProjectDao;

  private static final String NONE = "none";

  @GET
  @Path("projects")
  @Produces(APPLICATION_JSON)
  public Set<ReportItem> projects() throws StashException {
    List<Permission> permissions = commonDao.executeQuery(Permission.class,
            "select DISTINCT e from Permission e inner join fetch e.project inner join fetch e.user u where u.objectClass = :objClass",
            Collections.<String, Object>singletonMap("objClass", ObjectClass.user));
    return convertResult(permissions);
  }

  @GET
  @Path("grants")
  @Produces(APPLICATION_JSON)
  public Set<ReportItem> grants(@Context SecurityContext securityContext) throws StashException {
    List<Permission> permissions = commonDao.executeQuery(Permission.class,
            "select DISTINCT e from Permission e inner join fetch e.repo r inner join fetch e.user u inner join fetch r.project where u.objectClass = :objClass",
            Collections.<String, Object>singletonMap("objClass", ObjectClass.user));
    return convertResult(permissions);
  }

  @GET
  @Path("serviceStashMismatch")
  @Produces(APPLICATION_JSON)
  public List<PermissionMismatchItem> serviceStashMismatch() throws StashException, BrokenBarrierException, InterruptedException {
    return compareProjectsPermissions();
  }

  private List<PermissionMismatchItem> compareProjectsPermissions() throws StashException, BrokenBarrierException, InterruptedException {
    final ExecutorService executorService = Executors.newFixedThreadPool(10);
    final List<PermissionMismatchItem> result = Collections.synchronizedList(new LinkedList<>());
    Set<StashProject> stashProjects = loadStashProjects();
    List<Project> dbProjects = loadDbProjects();
    Collections.sort(dbProjects, new DbComparator<Project>());
    List<Project> projects = new ArrayList<>(dbProjects);
    final CountDownLatch latch = new CountDownLatch(2 * stashProjects.size());
    log.info("count down size for projects = {}", stashProjects.size());
    log.info("DB projects size = {}", projects.size());
    for (StashProject stashProject : stashProjects) {
      log.info("Start process stash project '{}'", stashProject.getKey());
      Project projectForSearch = new Project();
      projectForSearch.setExternalId(stashProject.getKey());
      int i = Collections.binarySearch(projects, projectForSearch, new DbComparator<Project>());
      final Project project = i < 0 ? null : projects.remove(i);
      if (project == null) {
        result.add(new PermissionMismatchItem(stashProject, null, false, false, true));
        log.info("DB project not found for stash project '{}' add 1 item to the result", stashProject.getKey());
        countDown(latch);
        countDown(latch);
        continue;
      } else {
        log.info("Found project match. DB project '{}', Stash project '{}'", project.getExternalId(), stashProject.getKey());
        log.info("DB projects size = {}", projects.size());
      }
      executorService.submit(() -> {
        try {
          List<PermissionMismatchItem> items = compareProjectUserPermissions(stashProject);
          result.addAll(items);
          log.info("User permissions for project. add {} items to result, db project '{}', stash project = '{}'", items.size(), project.getExternalId(), stashProject.getKey());
        } catch (StashException e) {
          log.error("error read data from stash", e);
        } finally {
          countDown(latch);
        }
      });
      executorService.submit(() -> {
        try {
          List<PermissionMismatchItem> items = compareProjectGroupPermissions(stashProject);
          result.addAll(items);
          log.info("Group permissions for projects. add {} items to result, db project '{}', stash project = '{}'", items.size(), project.getExternalId(), stashProject.getKey());
        } catch (StashException e) {
          log.error("error read data from stash", e);
        } finally {
          countDown(latch);
        }
      });
    }
    log.info("projects missing in stash {} add items to result", projects.size());
    Project currentProject = null;
    for (Project project : projects) {
      log.info("Project {} missing in stash", project.getExternalId());
      if (currentProject == null || !currentProject.getExternalId().equalsIgnoreCase(project.getExternalId())) {
        result.add(new PermissionMismatchItem(null, project, false, true, false));
      }
      currentProject = project;
    }
    latch.await();
    projects = new ArrayList<>(dbProjects);
    log.info("Start processing repos. DB projects size = {}", projects.size());
    for (StashProject stashProject : stashProjects) {
      Project projectForSearch = new Project();
      projectForSearch.setExternalId(stashProject.getKey());
      int i = Collections.binarySearch(projects, projectForSearch, new DbComparator<Project>());
      final Project project = i < 0 ? null : projects.remove(i);
      if (project == null) {
        log.info("Project {} does not exist in DB. Skip repos processing", stashProject.getKey());
        continue;
      }
      List<StashRepo> reposByProject = null;
      try {
        reposByProject = new ArrayList<>(stashProjectDao.getReposByProject(stashProject.getKey()));
      } catch (StashException e) {
        if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
          reposByProject = new ArrayList<>();
        } else {
          throw e;
        }
      }
      List<Repo> repos = new ArrayList<>(loadDbRepos(project));
      Collections.sort(repos, new DbComparator<Repo>());
      final CountDownLatch repoLatch = new CountDownLatch(2 * reposByProject.size());
      log.info("count down size for repos = {} for project {}", reposByProject.size(), stashProject.getKey());
      log.info("DB repos size {} for project {}", repos.size(), stashProject.getKey());
      for (StashRepo stashRepo : reposByProject) {
        Repo repoForSearch = new Repo();
        repoForSearch.setExternalId(stashRepo.getSlug());
        int idx = Collections.binarySearch(repos, repoForSearch, new DbComparator<Repo>());
        Repo repo = idx < 0 ? null : repos.remove(idx);
        log.info("Processing repo '{}', repo idx {}", stashRepo.getSlug(), idx);
        if (repo == null) {
          result.add(new PermissionMismatchItem(stashProject, project, stashRepo, null, false, false, true));
          log.info("Repo {} does not exists in db. Skip processing.  add 1 item to the result", stashRepo.getSlug());
          countDown(repoLatch);
          countDown(repoLatch);
          continue;
        } else {
          log.info("found repo match '{}', '{}', '{}', '{}'", repo.getExternalId(), stashRepo.getSlug(), project.getExternalId(), stashProject.getKey());
          log.info("DB repos size = {}", repos.size());
        }
        executorService.submit(() -> {
          try {
            List<PermissionMismatchItem> items = compareGrantUserPermissions(stashProject, stashRepo);
            result.addAll(items);
            log.info("User permissions for grant. add {} items to result, db project '{}', stash project = '{}', db repo = '{}', stash repo = '{}'",
                    items.size(), project.getExternalId(), stashProject.getKey(), repo.getExternalId(), stashRepo.getSlug());
          } catch (StashException e) {
            log.error("error read data from stash", e);
          } finally {
            countDown(repoLatch);
          }
        });
        executorService.submit(() -> {
          try {
            List<PermissionMismatchItem> items = compareGrantGroupPermissions(stashProject, stashRepo);
            result.addAll(items);
            log.info("Group permissions for grant. add {} items to result, db project '{}', stash project = '{}', db repo = '{}', stash repo = '{}'",
                    items.size(), project.getExternalId(), stashProject.getKey(), repo.getExternalId(), stashRepo.getSlug());
          } catch (StashException e) {
            log.error("error read data from stash", e);
          } finally {
            countDown(repoLatch);
          }
        });
      }
      log.info("add {} items to result", repos.size());
      Repo currentRepo = null;
      for (Repo repo : repos) {
        log.info("Repo '{}' missing in stash", repo.getExternalId());
        if (currentRepo == null || !currentRepo.getExternalId().equalsIgnoreCase(repo.getExternalId())) {
          result.add(new PermissionMismatchItem(stashProject, project, null, repo, false, true, false));
        }
        currentRepo = repo;
      }
      repoLatch.await();
    }
    log.info("total count = {}", result.size());
    return new ArrayList<>(result);
  }

  private void countDown(CountDownLatch latch) {
    latch.countDown();
  }

  private List<PermissionMismatchItem> compareProjectUserPermissions(StashProject stashProject) throws StashException {
    List<StashPermission> permissionsByProject = new ArrayList<>(stashProjectDao.getPermissionsByProject(null, stashProject.getKey()));
    Collections.sort(permissionsByProject, new Comparator<StashPermission>() {
      @Override
      public int compare(StashPermission o1, StashPermission o2) {
        return o1.getUser().getSlug().compareTo(o2.getUser().getSlug());
      }
    });
    List<Permission> permissions = loadDbProjectPermissions(stashProject.getKey(), ObjectClass.user);

    return getPermissionMismatchItems(stashProject, null, permissionsByProject, permissions, ObjectClass.user);
  }

  private List<PermissionMismatchItem> compareGrantUserPermissions(StashProject stashProject, StashRepo stashRepo) throws StashException {
    List<StashPermission> permissionsByRepo = new ArrayList<>(stashProjectDao.getPermissionsByRepo(null, stashProject.getKey(), stashRepo.getSlug()));
    Collections.sort(permissionsByRepo, new Comparator<StashPermission>() {
      @Override
      public int compare(StashPermission o1, StashPermission o2) {
        return o1.getUser().getSlug().compareTo(o2.getUser().getSlug());
      }
    });
    List<Permission> permissions = loadDbGrantPermissions(stashRepo.getSlug(), ObjectClass.user);

    return getPermissionMismatchItems(stashProject, stashRepo, permissionsByRepo, permissions, ObjectClass.user);
  }

  private List<PermissionMismatchItem> compareGrantGroupPermissions(StashProject stashProject, StashRepo stashRepo) throws StashException {
    List<StashPermission> permissionsByRepo = new ArrayList<>(stashProjectDao.getPermissionsByRepo(null, stashProject.getKey(), stashRepo.getSlug(), ObjectClass.group.getStashTarget()));
    Collections.sort(permissionsByRepo, new Comparator<StashPermission>() {
      @Override
      public int compare(StashPermission o1, StashPermission o2) {
        return o1.getGroup().getName().compareTo(o2.getGroup().getName());
      }
    });
    List<Permission> permissions = loadDbGrantPermissions(stashRepo.getSlug(), ObjectClass.group);

    return getPermissionMismatchItems(stashProject, stashRepo, permissionsByRepo, permissions, ObjectClass.group);
  }

  private List<PermissionMismatchItem> compareProjectGroupPermissions(StashProject stashProject) throws StashException {
    List<StashPermission> permissionsByProject = new ArrayList<>(stashProjectDao.getPermissionsByProject(null, stashProject.getKey(), ObjectClass.group.getStashTarget()));
    Collections.sort(permissionsByProject, new Comparator<StashPermission>() {
      @Override
      public int compare(StashPermission o1, StashPermission o2) {
        return o1.getGroup().getName().compareTo(o2.getGroup().getName());
      }
    });
    List<Permission> permissions = loadDbProjectPermissions(stashProject.getKey(), ObjectClass.group);

    return getPermissionMismatchItems(stashProject, null, permissionsByProject, permissions, ObjectClass.group);
  }

  private List<PermissionMismatchItem> getPermissionMismatchItems(StashProject stashProject, StashRepo stashRepo, List<StashPermission> permissionsByProject, List<Permission> permissions, ObjectClass objectClass) {
    List<StashPermission> stashPermissions = new ArrayList<>();
    List<Permission> permissionsByUser = new ArrayList<>();
    List<PermissionMismatchItem> result = new ArrayList<>();
    while (!permissionsByProject.isEmpty()) {
      StashUser stashUser = null;
      StashGroup stashGroup = null;
      stashPermissions.clear();
      permissionsByUser.clear();
      for (Iterator<StashPermission> it = permissionsByProject.iterator(); it.hasNext(); ) {
        StashPermission next = it.next();
        if (objectClass == ObjectClass.user && (stashUser == null || next.getUser().getSlug().equals(stashUser.getSlug()))) {
          stashPermissions.add(next);
          stashUser = next.getUser();
          it.remove();
        } else if (objectClass == ObjectClass.group && (stashGroup == null || next.getGroup().getName().equals(stashGroup.getName()))) {
          stashPermissions.add(next);
          stashGroup = next.getGroup();
          it.remove();
        } else {
          break;
        }
      }
      Permission permission = new Permission();
      permission.setUser(new User());
      if (stashUser != null) {
        permission.getUser().setLogin(stashUser.getSlug());
      } else {
        permission.getUser().setLogin(stashGroup.getName());
      }
      int i;
      while ((i = Collections.binarySearch(permissions, permission, new Comparator<Permission>() {
        @Override
        public int compare(Permission o1, Permission o2) {
          return o1.getUser().getLogin().compareToIgnoreCase(o2.getUser().getLogin());
        }
      })) >= 0) {
        permissionsByUser.add(permissions.remove(i));
      }
      List<String> dbPerm = new ArrayList<>();
      List<String> stashPerm = new ArrayList<>();
      boolean match = equalsPermissions(stashPermissions, permissionsByUser, stashPerm, dbPerm);
      result.add(new PermissionMismatchItem(stashProject, stashUser, null, stashRepo, stashGroup, match, stashPerm, dbPerm));
    }
    User user = null;
    permissionsByUser.clear();
    for (Iterator<Permission> it = permissions.iterator(); it.hasNext();) {
      Permission permission = it.next();
      if (user == null) {
        user = permission.getUser();
        permissionsByUser.add(permission);
        continue;
      }
      if (!user.getLogin().equals(permission.getUser().getLogin())) {
        List<String> dbPerm = new ArrayList<>();
        List<String> stashPerm = new ArrayList<>();
        boolean match = equalsPermissions(Collections.emptyList(), permissionsByUser, stashPerm, dbPerm);
        result.add(new PermissionMismatchItem(stashProject, null, permission.getUser(), stashRepo, null, match, stashPerm, dbPerm));
        permissionsByUser.clear();
        permissionsByUser.add(permission);
      }
      user = permission.getUser();
    }
    return result;
  }

  private boolean equalsPermissions(List<StashPermission> stashPermissions, List<Permission> permissions, List<String> stashPerms, List<String> perms) {
    if ((stashPermissions == null || stashPermissions.isEmpty()) && (permissions == null || permissions.isEmpty())) {
      return true;
    }
    stashPerms.addAll(stashPermissions.stream().map(s -> s.getPermission().toLowerCase().replace("project_", "").replace("repo_", "")).sorted().collect(Collectors.toList()));
    if (stashPerms.contains(PermissionRule.WRITE.name().toLowerCase())) {
      stashPerms.add(0, PermissionRule.READ.name().toLowerCase());
    }
    Set<PermissionRule> permissionRules = new HashSet<>();
    for (Permission permission : permissions) {
      permissionRules.addAll(PermissionsDao.convertPermissionsToSet(permission.getPermissions()));
    }
    perms.addAll(permissionRules.stream().map(s -> s.name().toLowerCase()).sorted().collect(Collectors.toList()));

    return CollectionUtils.isEqualCollection(stashPerms, perms);
  }

  private List<Permission> loadDbProjectPermissions(String projectKey, ObjectClass objectClass) {
    Map<String, Object> params = new HashMap<>();
    params.put("key", projectKey);
    params.put("objectClass", objectClass);
    return commonDao.executeQuery(Permission.class, "select e from Permission e " +
            "join fetch e.project p " +
            "join fetch e.user u " +
            "where u.objectClass = :objectClass " +
            "and p.externalId = :key " +
            "order by u.login", params);
  }

  private List<Permission> loadDbGrantPermissions(String projectKey, ObjectClass objectClass) {
    Map<String, Object> params = new HashMap<>();
    params.put("key", projectKey);
    params.put("objectClass", objectClass);
    return commonDao.executeQuery(Permission.class, "select e from Permission e " +
            "join fetch e.repo r " +
            "join fetch e.user u " +
            "where u.objectClass = :objectClass " +
            "and r.externalId = :key " +
            "order by u.login", params);
  }

  private List<Project> loadDbProjects() {
    List<Project> projects = commonDao.executeQuery(Project.class, "select e from Project e order by e.externalId, e.id", null);
    List<Project> result = new ArrayList<>(projects.size());
    Project currentProject = null;
    for (Project project : projects) {
      if (currentProject == null || !currentProject.getExternalId().equalsIgnoreCase(project.getExternalId())) {
        result.add(project);
      }
      currentProject = project;
    }
    return result;
  }

  private List<Repo> loadDbRepos(Project project) {
    List<Repo> repos = commonDao.executeQuery(Repo.class, "select e from Repo e where e.project = :project order by e.externalId, e.id", Collections.<String, Object>singletonMap("project", project));
    List<Repo> result = new ArrayList<>(repos.size());
    Repo currentRepo = null;
    for (Repo repo : repos) {
      if (currentRepo == null || !currentRepo.getExternalId().equalsIgnoreCase(repo.getExternalId())) {
        result.add(repo);
      }
      currentRepo = repo;
    }
    return result;
  }

  private Set<StashProject> loadStashProjects() throws StashException {
    try {
      return stashProjectDao.getProjects();
    } catch (StashException e) {
      if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
        return new HashSet<>();
      } else {
        throw e;
      }
    }
  }

  private Set<ReportItem> convertResult(List<Permission> permissions) {
    HashSet<ReportItem> result = new HashSet<>(permissions.size());
    for (Permission p : permissions) {
      result.add(new ReportItem(p.getRepo(), p.getProject(), p.getUser(), PermissionsDao.convertPermissionsToSet(p.getPermissions())));
    }
    return result;
  }

  public static class PermissionMismatchItem {
    private StashProject stashProject;
    private Project project;
    private StashRepo stashRepo;
    private Repo repo;
    private User user;
    private StashUser stashUser;
    private StashGroup stashGroup;
    private boolean match;
    private boolean missingInStash;
    private boolean missingInDb;
    private List<String> stashPermissions;
    private List<String> dbPermissions;

    public PermissionMismatchItem(StashProject stashProject, Project project, boolean match, boolean missingInStash, boolean missingInDb) {
      this.stashProject = stashProject;
      this.project = project;
      this.match = match;
      this.missingInStash = missingInStash;
      this.missingInDb = missingInDb;
    }

    public PermissionMismatchItem(StashProject stashProject, Project project, StashRepo stashRepo, Repo repo, boolean match, boolean missingInStash, boolean missingInDb) {
      this.stashProject = stashProject;
      this.project = project;
      this.stashRepo = stashRepo;
      this.repo = repo;
      this.match = match;
      this.missingInStash = missingInStash;
      this.missingInDb = missingInDb;
    }

    public PermissionMismatchItem(StashProject stashProject, StashUser stashUser, User user, boolean match) {
      this.stashProject = stashProject;
      this.stashUser = stashUser;
      this.user = user;
      this.match = match;
    }

    public PermissionMismatchItem(StashProject stashProject, StashUser stashUser, User user, StashRepo stashRepo, StashGroup stashGroup, boolean match, boolean missingInStash, boolean missingInDb) {
      this.stashProject = stashProject;
      this.stashUser = stashUser;
      this.stashRepo = stashRepo;
      this.user = user;
      this.stashGroup = stashGroup;
      this.match = match;
      this.missingInStash = missingInStash;
      this.missingInDb = missingInDb;
    }

    public PermissionMismatchItem(StashProject stashProject, StashUser stashUser, User user, StashRepo stashRepo, StashGroup stashGroup, boolean match, List<String> stashPermissions, List<String> dbPermissions) {
      this.stashProject = stashProject;
      this.stashUser = stashUser;
      this.stashRepo = stashRepo;
      this.user = user;
      this.stashGroup = stashGroup;
      this.match = match;
      this.stashPermissions = stashPermissions;
      this.dbPermissions = dbPermissions;
    }

    public String getProgram() {
      if (stashProject != null) {
        return stashProject.getName();
      }
      if (project != null) {
        return project.getExternalId();
      }
      return StringUtils.EMPTY;
    }

    public String getGrant() {
      if (stashRepo != null) {
        return stashRepo.getSlug();
      }
      if (repo != null) {
        return repo.getExternalId();
      }
      return StringUtils.EMPTY;
    }

    public String getGroup() {
      if (stashGroup != null) {
        return stashGroup.getName();
      }
      return user == null || user.getObjectClass() == ObjectClass.user ? "" : user.getLogin();
    }

    public String getUser() {
      if (stashUser != null) {
        return stashUser.getSlug();
      }
      return user == null || user.getObjectClass() == ObjectClass.group ? "" : user.getLogin();
    }

    public Boolean getMatch() {
      return match;
    }

    public Boolean getExistInStash() {
      if (match) {
        return null;
      }
      return !missingInStash;
    }

    public Boolean getExistInDb() {
      if (match) {
        return null;
      }
      return !missingInDb;
    }

    public String getStashPermissions() {
      if (match || existingMismatch()) {
        return StringUtils.EMPTY;
      }
      return CollectionUtils.isEmpty(stashPermissions) ? NONE : stashPermissions.stream().collect(Collectors.joining(","));
    }

    public String getDbPermissions() {
      if (match || existingMismatch()) {
        return StringUtils.EMPTY;
      }
      return CollectionUtils.isEmpty(dbPermissions) ? NONE : dbPermissions.stream().collect(Collectors.joining(","));
    }

    private boolean existingMismatch() {
      return missingInDb ^ missingInStash;
    }
  }

  public static class ReportItem {

    private Repo grant;
    private Project project;
    private User user;
    private Set<PermissionRule> rules;

    public ReportItem(Repo grant, Project project, User user, Set<PermissionRule> rules) {
      this.grant = grant;
      this.project = project;
      this.user = user;
      this.rules = rules;
    }

    public Repo getGrant() {
      return grant;
    }

    public void setGrant(Repo grant) {
      this.grant = grant;
    }

    public Project getProject() {
      return project;
    }

    public void setProject(Project project) {
      this.project = project;
    }

    public User getUser() {
      return user;
    }

    public void setUser(User user) {
      this.user = user;
    }

    public Set<PermissionRule> getRules() {
      return rules;
    }

    public void setRules(Set<PermissionRule> rules) {
      this.rules = rules;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ReportItem that = (ReportItem) o;

      if (grant != null ? !grant.equals(that.grant) : that.grant != null) return false;
      if (project != null ? !project.equals(that.project) : that.project != null) return false;
      return user.equals(that.user);

    }

    @Override
    public int hashCode() {
      int result = grant != null ? grant.hashCode() : 0;
      result = 31 * result + (project != null ? project.hashCode() : 0);
      result = 31 * result + user.hashCode();
      return result;
    }
  }

  public static class DbComparator<T extends BaseDBEntity> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
      return o1.getExternalId().toLowerCase().compareTo(o2.getExternalId().toLowerCase());
    }
  }

}
