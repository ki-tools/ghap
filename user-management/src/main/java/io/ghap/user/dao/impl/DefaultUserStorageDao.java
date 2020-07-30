package io.ghap.user.dao.impl;

import com.netflix.governator.annotations.Configuration;
import io.ghap.user.dao.UserStorageDao;
import io.ghap.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

/**
 * Created by arao on 3/7/16.
 */
public class DefaultUserStorageDao implements UserStorageDao {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Configuration("user.storage.workspace.root")
  private String workspaceRoot = "/data/workspace";// default

  @Configuration("user.storage.workspace.permissions.group")
  private String workspacePermissionGroup = "tomcat";// default



  @Configuration("user.storage.linuxhome.root")
  private String linuxHomesRoot = "/data/linuxhome";// default

  @Configuration("user.storage.linuxhome.permissions.group")
  private String linuxHomesPermissionGroup = "domain^users";// default


  @Override
  public void createUserWorkspaceStorage(User user) {
    String userName = user.getName();

    if (userName != null && userName.trim().length() > 0) {
      File workspaceRootDir = new File(getWorkspaceRoot());
      if (workspaceRootDir.exists()) {
        File userLevelRootDir = new File(workspaceRootDir, userName);

        createDirectoryIfNeeded(userLevelRootDir, userName, getWorkspacePermissionGroup());

      } else {
        log.info(String.format("Cannot create user workspace, since workspace root location <%s> does not exist",
                getWorkspaceRoot()));
      }
    }
  }

  @Override
  public void createUserLinuxHomeStorage(User user) {
    String userName = user.getName();

    if (userName != null && userName.trim().length() > 0) {
      File linuxHomesRootDir = new File(getLinuxHomesRoot());
      if (linuxHomesRootDir.exists()) {
        File userLevelRootDir = new File(linuxHomesRootDir, userName);

        createDirectoryIfNeeded(userLevelRootDir, userName, getLinuxHomesPermissionGroup());

      } else {
        log.info(String.format("Cannot create user workspace, since linux homes root location <%s> does not exist",
                getLinuxHomesRoot()));
      }
    }
  }

  private void createDirectoryIfNeeded(File directory, String userName, String userGroup) {
    File parentDirectory = directory.getParentFile();

    if (parentDirectory != null && (!parentDirectory.exists())) {
      createDirectoryIfNeeded(parentDirectory, userName, userGroup);
    }


    if (!directory.exists()) {

      if (!directory.mkdir()) {
        log.error("Cannot create folder \"" + directory.getAbsolutePath() + "\". Can write: " + directory.canWrite());
        throw new WebApplicationException(Response.status(500).entity("Cannot create folder with name \"" + directory.getName() + "\"").build());
      }

      log.debug("Created folder \"" + directory.getAbsolutePath() + "\"");

      log.debug("Set permissions on created folder \"" + directory.getAbsolutePath() + "\"");
      LocalStorageFileUtils.applyFilePermissions(directory, userName, userGroup);
    }
  }

  private String getWorkspaceRoot() {
    return workspaceRoot;
  }

  private String getWorkspacePermissionGroup() {
    return workspacePermissionGroup;
  }

  private String getLinuxHomesRoot() {
    return linuxHomesRoot;
  }

  private String getLinuxHomesPermissionGroup() {
    return linuxHomesPermissionGroup;
  }

  /**
   * Utility to set owner and permissions properly on the user workspace directories and files.
   */
  private static class LocalStorageFileUtils {

    private static final String CMD_CHANGE_FILE_OWNER = "sudo chown %s:%s %s";
    private static final String CMD_CHANGE_DIR_PERMS = "sudo chmod 2770 %s";
    private static final String CMD_CHANGE_FILE_PERMS = "sudo chmod 644 %s";

    private static final Logger LOG = LoggerFactory.getLogger(LocalStorageFileUtils.class);


    /**
     * Applies the proper ownership and permissions to the specified file or directory.
     *
     * @param fileItem the file or directory for which permissions and ownership needs to be set
     * @param owner    the desired owner
     */
    public static void applyFilePermissions(File fileItem, String owner, String group) {

      try {
        StringBuilder commandOutputBuffer = new StringBuilder();

        String filePath = fileItem.getCanonicalPath();

        executeBashCommand(String.format(CMD_CHANGE_FILE_OWNER, owner, group, filePath), commandOutputBuffer);

        if (fileItem.isDirectory()) {
          executeBashCommand(String.format(CMD_CHANGE_DIR_PERMS, filePath), commandOutputBuffer);
        } else {
          executeBashCommand(String.format(CMD_CHANGE_FILE_PERMS, filePath), commandOutputBuffer);
        }


        LOG.info(commandOutputBuffer.toString());

      } catch (IOException e) {
        LOG.error("Error occurred when applying permissions.", e);
      }
    }


    private static void executeBashCommand(String command, StringBuilder commandOutputBuffer) {

      StringBuilder individualCommandBuffer = new StringBuilder();

      int exitCode = OsCommandExecutionUtils.executeBashCommand(command, individualCommandBuffer);

      commandOutputBuffer.append("Executed command => [ ").append(command).append(" ] with exit code=")
              .append(exitCode).append("\n");

      commandOutputBuffer.append(individualCommandBuffer.toString());
    }

  }
}
