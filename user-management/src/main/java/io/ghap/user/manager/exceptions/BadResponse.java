package io.ghap.user.manager.exceptions;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BadResponse extends WebApplicationException{
    public BadResponse(Object entity){
        super(Response.status(400).entity(entity).build());
    }
}
