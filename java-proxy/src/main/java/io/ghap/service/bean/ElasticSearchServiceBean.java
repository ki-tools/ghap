package io.ghap.service.bean;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import io.ghap.logevents.*;
import io.ghap.model.ShinyApp;
import io.ghap.model.ShinyAppDescriptor;
import io.ghap.service.ElasticSearchService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ElasticSearchServiceBean implements ElasticSearchService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String SERVICE = "es";

    @Configuration("shiny_search_db.url")
    private String uri;
    private EventsClient client;

    @Override
    public void putAppsInElasticSearch(List<ShinyAppDescriptor> apps) throws IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            getClient().clearAll(Types.SHINY_APPS_SEARCH);
        } catch (IOException e) {
            log.info("can't clear previous apps. It seems it is empty", e);
        }
        if (CollectionUtils.isEmpty(apps)) {
            return;
        }
        for (ShinyAppDescriptor descriptor : apps) {
            Map<String, Object> convert = convert(descriptor);
            getClient().sendAsync(Types.SHINY_APPS_SEARCH, convert, new ResultHandler<JsonObject>() {
                @Override
                public void completed(JsonObject jsonObject) {
                    log.info("successfully send to elastic search {}", jsonObject);
                }

                @Override
                public void failed(Exception e) {
                    log.error("error send to elastic search", e);
                }
            });
        }
    }

    private EventsClient getClient() {
        if (client != null) {
            return client;
        }
        client = createClient();
        return client;
    }

    private synchronized EventsClient createClient() {
        if (client != null) {
            return client;
        }
        Map conf = new HashMap<>();
        conf.put("service", SERVICE);
        conf.put("uri", uri);
        return EventsClientFactory.build(DestinationType.ELASTICSEARCH, Indexes.SHINY_SEARCH, conf);
    }

    private Map<String, Object> convert(ShinyAppDescriptor shinyAppDescriptor) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String, Object> map = new HashMap<>();
        __convert(shinyAppDescriptor, map);
        return map;
    }

    private void __convert(Object o, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            String name = f.getName();
            if (f.getType() == ShinyApp.class) {
                __convert(PropertyUtils.getProperty(o, name), map);
                continue;
            } else if (o instanceof ShinyApp) {
                f.setAccessible(true);
                map.put(name, f.get(o));
            }
        }
    }
}
