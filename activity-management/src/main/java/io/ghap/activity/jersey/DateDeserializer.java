package io.ghap.activity.jersey;


import io.ghap.activity.bannermanagement.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DateDeserializer extends DefaultDateDeserializer {

    public DateDeserializer() {
        super(DefaultBannerService.DATE_FORMAT);
    }
}
