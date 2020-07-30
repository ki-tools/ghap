package io.ghap.jersey;

import io.ghap.banner.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class TimeSerializer extends DefaultDateSerializer {

    public TimeSerializer() {
        super(DefaultBannerService.TIME_FORMAT);
    }
}
