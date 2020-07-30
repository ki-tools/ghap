package io.ghap.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Path("test")
public class TestResource {

    @GET
    public String test() {
        return "OK";
    }
}
