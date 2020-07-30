package io.ghap.userdata.contribution.storage.local;

import com.netflix.governator.annotations.Configuration;
import io.ghap.userdata.contribution.Storage;
import io.ghap.userdata.contribution.storage.FileInfo;
import io.ghap.userdata.contribution.storage.FileStream;
import io.ghap.userdata.contribution.storage.ObjectListing;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LocalStorage implements Storage {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("userdata.local.users.rootPath")
    public String rootPath = "/data/workspace";// default

    @Override
    public void init() {
        rootPath = rootPath.replace('\\', '/');
        if( !rootPath.endsWith("/") ){
            rootPath += "/";
        }
    }

    @Override
    public ObjectListing listObjects(String folder, boolean recursive) throws IOException {
        File dir = new File(getRootPath(), folder);
        if( !dir.exists() || (dir.listFiles() == null)){
            return new ObjectListing(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        }
        //log.error("Try to get file list for " + dir.getAbsolutePath() + "\"(recursive: " + recursive + "). Exists: " + dir.exists());
        Collection<File> fileList = recursive ?
                FileUtils.listFilesAndDirs(dir, TrueFileFilter.INSTANCE, TrueFileFilter.TRUE):
                Arrays.asList(dir.listFiles());
        Collection<FileInfo> files = new ArrayList<>();
        Collection<FileInfo> folders = new ArrayList<>();
        for(File f:fileList){
            String path = f.getAbsolutePath();
            if( f.isFile() ){
                files.add( new FileInfo(path, f.length(), new Date(f.lastModified())) );
            }
            else if( !f.equals(dir) ){
                folders.add( new FileInfo(path) );
            }
        }
        log.debug("List objects from \"" + dir.getAbsolutePath() + "\"(recursive: " + recursive + "). " +
                "Files: " + files + ", " +
                "folders: " + folders);
        return new ObjectListing(files, folders);
    }

    @Override
    public void makeFolder(String userName, String path, String name) throws IOException {
        File dir = new File(new File(getRootPath(), path), name);

        createDirectoryIfNeeded(dir, userName);
    }

    private void createDirectoryIfNeeded(File directory, String userName) throws IOException {
      File parentDirectory = directory.getParentFile();

      if (parentDirectory != null && (!parentDirectory.exists())) {
        createDirectoryIfNeeded(parentDirectory, userName);
      }


      if (!directory.exists()) {

        if( !directory.mkdir() ){
          log.error("Cannot create folder \"" + directory.getAbsolutePath() + "\". Can write: " + directory.canWrite());
          throw new WebApplicationException(Response.status(500).entity("Cannot create folder with name \"" + directory.getName() + "\"").build());
        }

        log.debug("Created folder \"" + directory.getAbsolutePath() + "\"");

        log.debug("Set permissions on created folder \"" + directory.getAbsolutePath() + "\"");
        LocalStorageFileUtils.applyFilePermissions(directory, userName);
      }
    }


    @Override
    public InputStream loadFolder(String path, String root) throws IOException {
        Collection<File> fileList = FileUtils.listFiles(new File(getRootPath(), path), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        if (fileList == null || fileList.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final File file = File.createTempFile("load_folder_", ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (File f : fileList) {
                try(InputStream objectStream = new BufferedInputStream(new FileInputStream(f))) {
                    String key = f.getAbsolutePath().replace(getRootPath(), "");
                    if (root != null) {
                        key = key.replace(root + "/", "");
                    }
                    ZipEntry zipEntry = new ZipEntry(key);
                    zos.putNextEntry(zipEntry);
                    IOUtils.copy(objectStream, zos);
                }
            }
        } finally {
            zos.flush();
            zos.close();
        }
        return new FileInputStream(file) {
            @Override
            public void close() throws IOException {
                super.close();
                FileUtils.deleteQuietly(file);
            }
        };
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public void deleteObjects(String path) throws IOException {
        File f = new File(getRootPath(), path);
        LocalStorageFileUtils.applyFilePermissions(f);//fix dir/file permissions if dir/file was created via SSH
        if(f.isDirectory()) {
            /*
            Collection<File> fileList = FileUtils.listFiles(f, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            for(File curFile:fileList){
                LocalStorageFileUtils.setPermissionsForDelete(curFile);
            }
            */
            FileUtils.deleteDirectory(f);
        }
        else {
            f.delete();
        }
    }

    @Override
    public FileStream getFileStream(String path, boolean isAbsolutePath) throws IOException {
        File f = isAbsolutePath ? new File(path):new File(getRootPath(), path);
        LocalStorageFileUtils.applyFilePermissions(f);//fix file permissions if file was created via SSH
        String type = Files.probeContentType(Paths.get(f.getAbsolutePath()));//see: http://www.rgagnon.com/javadetails/java-0487.html
        return new FileStream(new BufferedInputStream(new FileInputStream(f)), type, f.length());
    }

    @Override
    public void putObject(String userName, String path, String name, File src) throws IOException {
        File destinationDirectory = new File(getRootPath(), path);

        createDirectoryIfNeeded(destinationDirectory, userName);

        LocalStorageFileUtils.applyFilePermissions(destinationDirectory);//fix dir permissions if dir was created via SSH
        File dest = new File(destinationDirectory, name);
        FileUtils.copyFile(src, dest);

        LocalStorageFileUtils.applyFilePermissions(dest, userName);
    }

    @Override
    public boolean isExists(String path, String name, boolean isFolder) {
        return new File(new File(getRootPath(), path), name).exists();
    }

    @Override
    public boolean isExists(String path) {
        return new File(getRootPath(), path).exists();
    }
}
