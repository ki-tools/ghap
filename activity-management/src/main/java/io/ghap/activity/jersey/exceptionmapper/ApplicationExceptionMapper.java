package io.ghap.activity.jersey.exceptionmapper;

import com.google.inject.Singleton;
import io.ghap.activity.exception.ApplicationException;
import io.ghap.activity.bannermanagement.model.DefaultResponse;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 */
@Provider
@Singleton
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(ApplicationException exception) {
        Response.ResponseBuilder rb = ConstraintViolationExceptionMapper.create(headers, HttpServletResponse.SC_BAD_REQUEST);
        rb.entity(new DefaultResponse(exception.getErrors()));
        return rb.build();
    }
}
