package io.ghap.activity.jersey;


import io.ghap.activity.bannermanagement.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DateSerializer extends DefaultDateSerializer {

    public DateSerializer() {
        super(DefaultBannerService.DATE_FORMAT);
    }
}
