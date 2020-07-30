package io.ghap.userdata.contribution.storage.local;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;

import static java.nio.file.attribute.PosixFilePermission.*;
import static java.util.Arrays.asList;

/**
 * Utility to set owner and permissions properly on the user workspace directories and files.
 * see: http://www.java2s.com/Tutorials/Java/Java_io/1030__Java_nio_File_Owner_Permissions.htm
 */
class LocalStorageFileUtils {

  private static final String GROUP = "tomcat";
  private static final List<String> CMD_CHANGE_FILE_OWNER = asList("sudo", "chown");
  private static final List<String> CMD_CHANGE_DIR_PERMS = asList("sudo", "chmod", "2770");
  private static final List<String> CMD_CHANGE_FILE_PERMS = asList("sudo", "chmod", "644");

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorageFileUtils.class);

  public static void applyFilePermissions(File f) throws IOException {
    applyFilePermissions(f, null);
  }

  /**
   * Applies the proper ownership and permissions to the specified file or directory.
   * @param fileItem the file or directory for which permissions and ownership needs to be set
   * @param owner the desired owner. If owner is null then only GROUP owner be defined
   */
  public static void applyFilePermissions(File fileItem, String owner) throws IOException {

    if(SystemUtils.IS_OS_WINDOWS){
      return;
    }

    //Path path = fileItem.toPath();
    StringBuilder commandOutputBuffer = new StringBuilder();
    String filePath = fileItem.getCanonicalPath();

    try {
      if(owner == null) {
        executeBashCommand(toCommand(CMD_CHANGE_FILE_OWNER, ":" + GROUP, filePath), commandOutputBuffer);
        LOG.info("Change file group owner response: " + commandOutputBuffer.toString());
      }
      else {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(owner);// @throws  UserPrincipalNotFoundException the principal does not exist

        //Files.setOwner(path, userPrincipal);
        UserPrincipal currentOwner = Files.getFileAttributeView(fileItem.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).getOwner();
        LOG.info("Change \"" + fileItem.getAbsolutePath() + "\" file owner from \"" + currentOwner + "\" to \"" + userPrincipal + "\"");
        //Files.setOwner(path, userPrincipal);
        //Files.getFileAttributeView(fileItem.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(userPrincipal);
        executeBashCommand(toCommand(CMD_CHANGE_FILE_OWNER, owner + ":" + GROUP, filePath), commandOutputBuffer);
        LOG.info("Change file owner response: " + commandOutputBuffer.toString());
      }
      commandOutputBuffer.setLength(0);

    } catch (IOException e) {
      LOG.error("Error occurred when set owner. " + e);
      return;
    }

    if (fileItem.isDirectory()) {

      /*
      Set<PosixFilePermission> permissions = EnumSet.of(
              OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
              GROUP_READ, GROUP_WRITE, GROUP_EXECUTE
      );

      Files.setPosixFilePermissions(path, permissions);
      */
      //Files.getFileAttributeView(fileItem.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setPermissions(permissions);

      // use Linux command line tool since it is impossible to set SGID with Java API
      /*
      StringBuilder commandOutputBuffer = new StringBuilder();
      String filePath = fileItem.getCanonicalPath();
      */
      executeBashCommand(toCommand(CMD_CHANGE_DIR_PERMS, filePath), commandOutputBuffer);

    } else {
      /*
      Set<PosixFilePermission> permissions = EnumSet.of(
              OWNER_READ, OWNER_WRITE,
              GROUP_READ, GROUP_WRITE,
              OTHERS_READ);

      Files.setPosixFilePermissions(path, permissions);
      */
      //Files.getFileAttributeView(fileItem.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setPermissions(permissions);
      executeBashCommand(toCommand(CMD_CHANGE_FILE_PERMS, filePath), commandOutputBuffer);
    }
    LOG.info("Set permission response: " + commandOutputBuffer.toString());
  }

  private static String[] toCommand(Collection<String> list, String... args){
    Objects.requireNonNull(list);
    if(args == null || args.length == 0){
      return list.toArray(new String[list.size()]);
    }
    List<String> result = new ArrayList<>(list.size() + args.length);
    result.addAll(list);
    Collections.addAll(result, args);
    return result.toArray(new String[result.size()]);
  }

  private static void executeBashCommand(String[] command, StringBuilder commandOutputBuffer) {

    try {
      Runtime runtime = Runtime.getRuntime();

      Process process = runtime.exec(command);
      int exitCode = process.waitFor();

      commandOutputBuffer.append("Executed command => [ ").append(String.join(" ", command)).append(" ] with exit code=")
              .append(exitCode).append("\n");

      // Grab output
      readStreamIntoBuffer(process.getInputStream(), commandOutputBuffer);

      // Grab error outputs (if any)
      readStreamIntoBuffer(process.getErrorStream(), commandOutputBuffer);
    } catch (IOException | InterruptedException e) {
      LOG.error("Error occured when executing bash command", e);
    }
  }

  private static void readStreamIntoBuffer(InputStream inputStream, StringBuilder targetBuffer) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    try {
      String line = "";
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


}
