package io.ghap.project.exception;

import io.ghap.project.model.*;
import io.ghap.project.model.Error;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 */
public class EntityNotFoundException extends ApplicationException {

    private final static Set<Error> ERRORS = new HashSet<>();

    static {
        ERRORS.add(new Error(-1, "Entity not found by id"));
    }

    public EntityNotFoundException() {
        super(ERRORS);
    }

    public EntityNotFoundException(Set<io.ghap.project.model.Error> errors) {
        super(errors);
    }

    public EntityNotFoundException(Response response, Set<Error> errors) {
        super(response, errors);
    }

    public EntityNotFoundException(int status, Set<Error> errors) {
        super(status, errors);
    }

    public EntityNotFoundException(Response.Status status, Set<Error> errors) {
        super(status, errors);
    }

    public EntityNotFoundException(Throwable cause, Set<Error> errors) {
        super(cause, errors);
    }

    public EntityNotFoundException(Throwable cause, Response response, Set<Error> errors) {
        super(cause, response, errors);
    }

    public EntityNotFoundException(Throwable cause, int status, Set<Error> errors) {
        super(cause, status, errors);
    }

    public EntityNotFoundException(Throwable cause, Response.Status status, Set<Error> errors) {
        super(cause, status, errors);
    }
}
