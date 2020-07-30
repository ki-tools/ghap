package io.ghap.visualization.publish.search;

import io.ghap.logevents.Types;
import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SearchClient {

    void refresh() throws IOException;

    void clearAll(Types eventType) throws IOException;

    void clearAll() throws IOException;

    void close();

    List<SearchResult.Hit<Map, Void>> read(Types recordType, String query) throws IOException;
}
