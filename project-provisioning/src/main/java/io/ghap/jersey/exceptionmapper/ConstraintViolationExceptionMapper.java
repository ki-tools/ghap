package io.ghap.jersey.exceptionmapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.ghap.project.model.*;
import io.ghap.project.model.Error;
import org.apache.commons.collections.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 */
@Provider
@Singleton
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    private HttpHeaders headers;

    @Inject
    @Named("defaultMessages")
    private ResourceBundle messages;

    @Inject
    @Named("defaultMessageCodes")
    private ResourceBundle codes;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Response.ResponseBuilder rb = create(headers, HttpServletResponse.SC_BAD_REQUEST);
        rb.entity(new DefaultResponse(toErrors(exception)));
        return rb.build();
    }

    private Set<Error> toErrors(ConstraintViolationException e) {
        if (CollectionUtils.isEmpty(e.getConstraintViolations())) {
            return new HashSet<>();
        }
        Set<Error> errors = new HashSet<>();
        for (ConstraintViolation violation : e.getConstraintViolations()) {
            String messageKey = getMessageKey(violation);
            errors.add(new Error(Integer.valueOf(codes.getString(messageKey)), violation.getPropertyPath().toString() + " " + violation.getMessage()));
        }
        return errors;
    }

    private String getMessageKey(ConstraintViolation violation) {
        return violation.getRootBeanClass().getSimpleName() + "." + violation.getPropertyPath().toString();
    }

    public static Response.ResponseBuilder create(HttpHeaders headers, int responseStatus) {
        List<MediaType> mediaTypes = headers.getAcceptableMediaTypes();
        Response.ResponseBuilder rb = Response.status(responseStatus);
        if (mediaTypes!=null && mediaTypes.contains(MediaType.APPLICATION_XML_TYPE)) {
            rb.type(MediaType.APPLICATION_XML);
        } else {
            rb.type(MediaType.APPLICATION_JSON);
        }
        return rb;
    }
}
