package io.ghap.userdata.logevents;

public class FileEvent {

    private final String name;
    private final long size;
    private final String contentType;

    public FileEvent(final String name, final long size, final String contentType) {
        this.name = name;
        this.size = size;
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return "FileEvent{" +
                "name='" + name + '\'' +
                '}';
    }
}
