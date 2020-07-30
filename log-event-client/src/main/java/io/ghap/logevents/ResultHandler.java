package io.ghap.logevents;

public interface ResultHandler<T> {

    void completed(T result);

    void failed(Exception ex);

}
