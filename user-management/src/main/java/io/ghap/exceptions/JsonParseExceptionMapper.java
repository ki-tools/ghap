package io.ghap.exceptions;

import org.codehaus.jackson.JsonParseException;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;

@Provider
@Singleton
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(JsonParseException e) {
        return status(BAD_REQUEST).entity(e.getMessage()).build();
    }
}
