package io.ghap.s3;

import com.amazonaws.AmazonClientException;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.status;

@Provider
@Singleton
public class AmazonClientExceptionMapper implements ExceptionMapper<AmazonClientException> {
    @Override
    public Response toResponse(AmazonClientException e) {
        return status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
}
