package io.ghap.jersey.exceptionmapper;

import com.google.inject.Singleton;
import io.ghap.project.exception.ApplicationException;
import io.ghap.project.model.DefaultResponse;

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
        Response.ResponseBuilder rb = ConstraintViolationExceptionMapper.create(headers, exception.getResponse().getStatus());
        rb.entity(new DefaultResponse(exception.getErrors()));
        return rb.build();
    }
}
