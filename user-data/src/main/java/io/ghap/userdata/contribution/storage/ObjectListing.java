package io.ghap.userdata.contribution.storage;

import java.util.Collection;

public class ObjectListing {
    private Collection<FileInfo> files;
    private Collection<FileInfo> folders;

    public ObjectListing(Collection<FileInfo> files, Collection<FileInfo> folders){
        this.files = files;
        this.folders = folders;
    }

    public Collection<FileInfo> getFiles() {
        return files;
    }

    public Collection<FileInfo> getFolders() {
        return folders;
    }
}
