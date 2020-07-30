package io.ghap.activity.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Set;
import io.ghap.activity.bannermanagement.model.Error;

public class ApplicationException extends WebApplicationException {

    private Set<Error> errors;

    public ApplicationException(Set<Error> errors) {
        this.errors = errors;
    }

    public ApplicationException(Response response, Set<Error> errors) {
        super(response);
        this.errors = errors;
    }

    public ApplicationException(int status, Set<Error> errors) {
        super(status);
        this.errors = errors;
    }

    public ApplicationException(Response.Status status, Set<Error> errors) {
        super(status);
        this.errors = errors;
    }

    public ApplicationException(Throwable cause, Set<Error> errors) {
        super(cause);
        this.errors = errors;
    }

    public ApplicationException(Throwable cause, Response response, Set<Error> errors) {
        super(cause, response);
        this.errors = errors;
    }

    public ApplicationException(Throwable cause, int status, Set<Error> errors) {
        super(cause, status);
        this.errors = errors;
    }

    public ApplicationException(Throwable cause, Response.Status status, Set<Error> errors) {
        super(cause, status);
        this.errors = errors;
    }

    public Set<Error> getErrors() {
        return errors;
    }
}
