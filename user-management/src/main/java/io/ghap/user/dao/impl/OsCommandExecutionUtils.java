package io.ghap.user.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility containing methods to execute OS commands.
 */
class OsCommandExecutionUtils {

  private static final Logger LOG = LoggerFactory.getLogger(OsCommandExecutionUtils.class);


  /**
   * Executes the specified bash command.
   *
   * @param command the command to execute.
   * @param commandOutputBuffer the contents of the command output and error streams.
   * @return he exit value of the command call. By convention, the value 0 indicates normal termination
   */
  public static int executeBashCommand(String command, StringBuilder commandOutputBuffer) {
    int exitCode = -1;

    try {
      Runtime runtime = Runtime.getRuntime();

      Process process = runtime.exec(command);
      exitCode = process.waitFor();

      // Grab output
      readStreamIntoBuffer(process.getInputStream(), commandOutputBuffer);

      // Grab error outputs (if any)
      readStreamIntoBuffer(process.getErrorStream(), commandOutputBuffer);


    } catch (IOException | InterruptedException e) {
      LOG.error("Error occured when executing bash command", e);
    }

    return exitCode;
  }

  private static void readStreamIntoBuffer(InputStream inputStream, StringBuilder targetBuffer) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        targetBuffer.append(line).append("\n");
      }
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.info("Unable to close the input stream", e);
      }
    }
  }

  /**
   * Gets the unix uid for the specified username.
   * @param username the user's username in unix
   * @return the user's uid
   */
  public static String getUserUid(String username) {
    String uid = null;

    String commandToExecute = String.format("id  -u %s", username);

    StringBuilder commandOutputBuffer = new StringBuilder();

    int exitCode = executeBashCommand(commandToExecute, commandOutputBuffer);
    if (exitCode == 0) {
      uid = commandOutputBuffer.toString().trim();
      LOG.debug(String.format("The uid for user <%s> = <%s>", username, uid));
    } else {
      LOG.error(String.format("Could not find uid for user <%s> =>\t%s", username, commandOutputBuffer.toString()));
    }
    return uid;
  }

  /**
   * Gets the unix gid for the specified username.
   * @param username the user's username in unix
   * @return the user's gid
   */
  public static String getUserGid(String username) {
    String gid = null;

    String commandToExecute = String.format("id  -g %s", username);

    StringBuilder commandOutputBuffer = new StringBuilder();

    int exitCode = executeBashCommand(commandToExecute, commandOutputBuffer);
    if (exitCode == 0) {
      gid = commandOutputBuffer.toString().trim();
      LOG.debug(String.format("The gid for user <%s> = <%s>", username, gid));
    } else {
      LOG.error(String.format("Could not find gid for user <%s> =>\t%s", username, commandOutputBuffer.toString()));
    }
    return gid;
  }
}


