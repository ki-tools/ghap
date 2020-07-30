package io.ghap.project.dao;

import io.ghap.project.domain.*;
import io.ghap.project.model.Commit;
import io.ghap.project.model.StashException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StashProjectDao {
    Set<StashProject> getProjects() throws StashException;

    StashProject getProject(String key) throws StashException;

    StashProject createProject(StashProject stashProject) throws StashException;

    StashProject updateProject(StashProject stashProject) throws StashException;

    void deleteProject(String key) throws StashException;

    Set<StashRepo> getReposByProject(String projectKey) throws StashException;

    StashRepo getRepo(String projectKey, String slug) throws StashException;

    StashRepo createRepo(String projectKey, StashRepo stashRepo) throws StashException;

    StashRepo updateRepo(String projectKey, StashRepo stashRepo) throws StashException;

    void deleteRepo(String projectKey, String slug) throws StashException;

    void grantPermissionsByProject(Permission permission, String permissions) throws StashException;

    void revokePermissionsByProject(Permission permission) throws StashException;

    void grantPermissionsByRepo(Permission permission, String permissions) throws StashException;

    void revokePermissionsByRepo(Permission permission) throws StashException;

    void updatePermission(Permission permission) throws StashException;

    List<Commit> getHistoryByRepo(String projectKey, String slug) throws StashException;

    Set<StashPermission> getPermissionsByProject(String userName, String projectKey) throws StashException;

    Set<StashPermission> getPermissionsByProject(String userName, String projectKey, String groupOrUser) throws StashException;

    Set<StashPermission> getPermissionsByRepo(String userName, String projectKey, String repoSlug) throws StashException;

    Set<StashPermission> getPermissionsByRepo(String userName, String projectKey, String repoSlug, String groupOrUser) throws StashException;

    String getLogin();

    void ping() throws StashException;

    boolean isFileExistsInStash(String fileName) throws StashException;

    Map<String, String> isFileExistsInStash(Set<String> fileNames) throws StashException;
}
