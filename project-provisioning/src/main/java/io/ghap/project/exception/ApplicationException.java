package io.ghap.project.exception;

import io.ghap.project.model.Error;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author MaximTulupov@certara.com
 */
public class ApplicationException extends WebApplicationException {

    private Set<Error> errors;

    public ApplicationException(Set<Error> errors) {
        super(HttpServletResponse.SC_BAD_REQUEST);
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
        super(cause, HttpServletResponse.SC_BAD_REQUEST);
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
