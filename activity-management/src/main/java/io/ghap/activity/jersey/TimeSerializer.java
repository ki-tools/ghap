package io.ghap.activity.jersey;


import io.ghap.activity.bannermanagement.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class TimeSerializer extends DefaultDateSerializer {

    public TimeSerializer() {
        super(DefaultBannerService.TIME_FORMAT);
    }
}
