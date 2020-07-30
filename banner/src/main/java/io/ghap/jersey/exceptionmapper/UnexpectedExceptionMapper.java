package io.ghap.jersey.exceptionmapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.ghap.banner.model.*;
import io.ghap.banner.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 */
@Provider
@Singleton
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Context
    private HttpHeaders headers;

    @Inject
    @Named("defaultMessages")
    private ResourceBundle messages;

    @Inject
    @Named("defaultMessageCodes")
    private ResourceBundle codes;

    @Override
    public Response toResponse(Throwable exception) {
        log.error("unexpected error", exception);
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }
        Response.ResponseBuilder rb = ConstraintViolationExceptionMapper.create(headers, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        Set<Error> errors = new HashSet<>();
        errors.add(new Error(Integer.valueOf(codes.getString("unexpected.error")), messages.getString("unexpected.error") + " " + exception.getMessage()));
        rb.entity(new DefaultResponse(errors));
        return rb.build();
    }
}
