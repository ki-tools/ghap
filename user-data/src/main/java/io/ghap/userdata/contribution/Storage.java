package io.ghap.userdata.contribution;




import io.ghap.userdata.contribution.storage.FileStream;
import io.ghap.userdata.contribution.storage.ObjectListing;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface Storage {

    @PostConstruct
    void init();

    ObjectListing listObjects(String folder, boolean recursive) throws IOException;

    void makeFolder(String userName, String path, String name) throws IOException;

    InputStream loadFolder(String path, String root) throws IOException;

    String getRootPath();

    void deleteObjects(String path) throws IOException;

    FileStream getFileStream(String path, boolean isAbsolutePath) throws IOException;

    void putObject(String userName, String path, String name, File srcFile) throws IOException;

    boolean isExists(String path, String name, boolean isFolder);

    boolean isExists(String path);

}
