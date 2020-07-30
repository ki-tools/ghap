package io.ghap.jersey;

import io.ghap.banner.manager.DefaultBannerService;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DateDeserializer extends DefaultDateDeserializer {

    public DateDeserializer() {
        super(DefaultBannerService.DATE_FORMAT);
    }
}
