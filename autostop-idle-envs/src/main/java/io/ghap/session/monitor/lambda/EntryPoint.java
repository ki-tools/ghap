package io.ghap.session.monitor.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.JsonSyntaxException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 */
public class EntryPoint implements RequestHandler<SNSEvent, Response> {
    private Logger logger = LoggerFactory.getLogger(EntryPoint.class);


    @Override
    public Response handleRequest(SNSEvent input, Context context) {

        logger.info("Begin handling request");

        List<SNSEvent.SNSRecord> records = input.getRecords();
        if (records == null || records.isEmpty()) {
            logger.info("No records within the SNS event. Nothing to do");
            //TODO send error
            return new Response();
        }

        RequestMessageHelper requestMessageHelper = new RequestMessageHelper();

        for (SNSEvent.SNSRecord record : records) {
            context.getLogger().log("The messageId = " + record.getSNS().getMessageId() + "\n");
            context.getLogger().log("The message timestamp = " + record.getSNS().getTimestamp() + "\n");
            context.getLogger().log(record.getSNS().getMessage() + "\n");


            try {
                CloudWatchMessage message = requestMessageHelper.getCloudWatchMessage(record);

                sendRequestToScheduleStopOfIdleResource(message);

            } catch (JsonSyntaxException e) {
                logger.error("The SNS message was not from a CloudWatch Alarm.", e);
            }
        }


        Response response = new Response();
        return response;
    }

    private void sendRequestToScheduleStopOfIdleResource(CloudWatchMessage cloudWatchMessage) {
        TargetServiceEndpointResolver endpointResolver = new TargetServiceEndpointResolver();
        String restEndpoint = endpointResolver.buildScheduleStopIdleResourcesRestEndpoint();

        logger.info(String.format("Initiating a call to the REST endpoint <%s>", restEndpoint));


        if (restEndpoint != null) {
            try {
                ClientResponse response = create().resource(restEndpoint).accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                        .put(ClientResponse.class, cloudWatchMessage);
                logger.info(String.format("status = %d", response.getStatus()));

            } catch (Throwable e) {
                logger.error("An error occurred when calling the endpoint", e);
            }
        }
    }

    private Client create() {
        Client client = Client.create(getConfig());
        return client;
    }

    private com.sun.jersey.api.client.config.ClientConfig getConfig() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig(); // SSL configuration
        try {
            config.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                    return true;
                }
            }, getSSLContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        config.getClasses().add(JacksonJsonProvider.class);
        return config;
    }

    // disable cert verification
    private SSLContext getSSLContext() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        return ctx;
    }
}
