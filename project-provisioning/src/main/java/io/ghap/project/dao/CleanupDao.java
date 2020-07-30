package io.ghap.project.dao;

/**
 */
public interface CleanupDao {
    int cleanupUsers(String accessToken);

    int cleanupGroups(String accessToken);

    void runCleanupProcedure();

    void scheduleCleanupProcedure();
}
