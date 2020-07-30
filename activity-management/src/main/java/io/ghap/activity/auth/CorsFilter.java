package io.ghap.activity.auth;


import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class CorsFilter implements ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Response.ResponseBuilder responseBuilder = Response.fromResponse(response.getResponse());

        String origin = request.getHeaderValue("origin");

        responseBuilder.header("Access-Control-Allow-Origin", origin == null ? "*":origin)
                .header("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS, PUT, DELETE, X-XSRF-TOKEN")
                .header("Access-Control-Allow-Credentials", "true");

        String reqHead = request.getHeaderValue("Access-Control-Request-Headers");

        if(null != reqHead && !reqHead.isEmpty()){
            responseBuilder.header("Access-Control-Allow-Headers", reqHead);
        }
        else {
            responseBuilder.header("Access-Control-Allow-Headers",
                    "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With");
        }

        response.setResponse(responseBuilder.build());
        return response;
    }
}
