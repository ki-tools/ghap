package io.ghap.jersey;

import io.ghap.banner.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DateSerializer extends DefaultDateSerializer {

    public DateSerializer() {
        super(DefaultBannerService.DATE_FORMAT);
    }
}
