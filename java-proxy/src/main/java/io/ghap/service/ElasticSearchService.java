package io.ghap.service;

import io.ghap.model.ShinyAppDescriptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface ElasticSearchService {
    void putAppsInElasticSearch(List<ShinyAppDescriptor> apps) throws IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
}
