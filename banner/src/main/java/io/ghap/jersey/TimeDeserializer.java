package io.ghap.jersey;

import io.ghap.banner.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class TimeDeserializer extends DefaultDateDeserializer {

    public TimeDeserializer() {
        super(DefaultBannerService.TIME_FORMAT);
    }
}
