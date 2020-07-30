package io.ghap.service;

import io.ghap.model.ShinyAppDescriptor;

import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface PrepareRulesService {
    void scheduleRegistryUpdate();

    List<ShinyAppDescriptor> getRegistryFile();
}
