package io.ghap.user.manager.exceptions;

import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;

@Provider
@Singleton
public class UnrecognizedPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {
    @Override
    public Response toResponse(UnrecognizedPropertyException e) {
        return status(BAD_REQUEST).entity(e.getMessage()).build();
    }
}
