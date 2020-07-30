package io.ghap.logevents;

import java.util.Map;

public class EventsClientFactory {
    public static EventsClient build(DestinationType type, Indexes index, Map config){
        switch (type){
            case ELASTICSEARCH:
                return new ElasticSearchClient(index, config);
            default:
                throw new UnsupportedOperationException("Cannot find " + type + " client type");
        }
    }
}
