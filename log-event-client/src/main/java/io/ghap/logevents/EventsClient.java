package io.ghap.logevents;

import com.google.gson.JsonObject;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.SearchResult;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface EventsClient extends AutoCloseable, Closeable {
    /**
     * Each client should authenticate during first "send"
     * @param data
     * @throws IOException
     */
    void send(Types eventType, Map data) throws IOException;

    void sendAsync(Types eventType, Map data, ResultHandler<JsonObject> jestResultHandler) throws IOException;

    void refresh() throws IOException;

    void clearAll(Types eventType) throws IOException;

    void clearAll() throws IOException;

    List<SearchResult.Hit<EventItem, Void>> read(Types eventType, Date start, Date end) throws IOException;
}
