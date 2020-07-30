package io.ghap.userdata.contribution.storage;

import java.io.InputStream;

public class FileStream {
    private final InputStream is;
    private final String contentType;
    private final long size;

    public FileStream(InputStream is, String contentType, long size) {
        this.is = is;
        this.contentType = contentType;
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() {
        return is;
    }

    public long getSize() {
        return size;
    }
}
