package io.ghap.userdata.contribution.storage;

import java.io.File;
import java.util.Date;
import java.util.Objects;

public class FileInfo {
    private final String path;
    private String name;
    private final Long size;
    private final Date lastModified;

    public FileInfo(String path) {
        this(path, null, null);
    }

    public FileInfo(String path, Long size, Date lastModified) {
        this.path = Objects.requireNonNull(path, "Absolute file path should be provided");
        this.size = size;
        this.lastModified = lastModified;
        this.name = new File(path).getName();
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
