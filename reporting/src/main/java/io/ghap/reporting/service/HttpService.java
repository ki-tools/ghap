package io.ghap.reporting.service;

import com.sun.jersey.api.client.Client;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface HttpService {

    Client create(String accessToken);
}
