package io.ghap.project.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.domain.Permission;
import io.ghap.project.domain.Project;
import io.ghap.project.domain.Repo;
import io.ghap.project.model.Error;
import io.ghap.project.model.StashException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class SyncDbWithGitService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private StashProjectDao stashProjectDao;

    @Inject
    private StashExceptionService stashExceptionService;

    @Inject
    private CommonPersistDao commonPersistDao;

    public List<Error> sync() {
        List<Project> projects = commonPersistDao.readAll(Project.class);
        List<Error> errors = new ArrayList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        log.info("project size = {}", projects.size());
        for (Project project : projects) {
            log.info("Start sync {}, {}", project.getExternalId(), project.getId());
            errors.addAll(syncProject(project, executorService));
        }
        return errors;
    }

    private List<Error> syncProject(Project project, final ExecutorService executorService) {
        final List<Error> errors = Collections.synchronizedList(new LinkedList<>());
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", project.getId());
        List<Permission> permissions = commonPersistDao.executeQuery(Permission.class,
                "select p from Permission p inner join fetch p.project pr " +
                        "inner join fetch p.user where pr.id = :id", params);
        for (Permission permission : permissions) {
            try {
                stashProjectDao.updatePermission(permission);
            } catch (Exception e) {
                if (e instanceof StashException) {
                    errors.addAll(stashExceptionService.toWebErrors(((StashException) e)));
                } else {
                    log.error("error sync project " + project.getExternalId(), e);
                    errors.add(new Error(-1, e.getMessage()));
                }
            }
        }
        HashMap<String, Object> params1 = new HashMap<>();
        params1.put("project", project);
        List<Repo> repos = commonPersistDao.executeQuery(Repo.class, "select r from Repo r where r.project = :project", params1);
        final CountDownLatch latch = new CountDownLatch(repos.size());
        log.info("repos size = {}", repos.size());
        for (final Repo repo : repos) {
            log.info("Start sync repo {}, {}", repo.getExternalId(), repo.getId());
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        errors.addAll(syncRepo(repo));
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("latch interrupted for project " + project.getExternalId(), e);
        }
        return errors;
    }

    private List<Error> syncRepo(Repo repo) {
        List<Error> errors = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", repo.getId());
        List<Permission> permissions = commonPersistDao.executeQuery(Permission.class,
                "select p from Permission p inner join fetch p.repo pr " +
                        "inner join fetch p.user where pr.id = :id", params);
        for (Permission permission : permissions) {
            try {
                stashProjectDao.updatePermission(permission);
            } catch (Exception e) {
                if (e instanceof StashException) {
                    errors.addAll(stashExceptionService.toWebErrors(((StashException) e)));
                } else {
                    log.error("error sync repo " + repo.getExternalId(), e);
                    errors.add(new Error(-1, e.getMessage()));
                }
            }
        }
        return errors;
    }
}
