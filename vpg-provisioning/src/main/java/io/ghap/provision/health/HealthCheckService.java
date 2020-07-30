package io.ghap.provision.health;

import java.util.Map;

/**
 */
public interface HealthCheckService {
    Map<String, String> checkHealth();

    boolean isCheckSuccess(Map<String, String> result);
}
