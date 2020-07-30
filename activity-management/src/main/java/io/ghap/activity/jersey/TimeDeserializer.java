package io.ghap.activity.jersey;


import io.ghap.activity.bannermanagement.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class TimeDeserializer extends DefaultDateDeserializer {

    public TimeDeserializer() {
        super(DefaultBannerService.TIME_FORMAT);
    }
}
