package io.ghap.user.manager.exceptions;

import io.ghap.user.exceptions.EntryNotFound;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.status;

@Provider
@Singleton
public class EntryNotFoundExceptionMapper implements ExceptionMapper<EntryNotFound> {
    @Override
    public Response toResponse(EntryNotFound e) {
        return status(NOT_FOUND).entity(e.getMessage()).build();
    }
}

