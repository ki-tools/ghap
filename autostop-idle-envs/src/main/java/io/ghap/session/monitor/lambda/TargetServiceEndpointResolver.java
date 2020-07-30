package io.ghap.session.monitor.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

class TargetServiceEndpointResolver {


    private static class ApplicationPropertyHolder {

        private final Properties applicationProperties;

        private ApplicationPropertyHolder() {
            applicationProperties = loadProperties();
        }

        private Properties loadProperties() {
            Properties properties = new Properties();

            InputStream in = getClass().getResourceAsStream("/application.properties");
            try {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return properties;
        }

        public Properties getApplicationProperties() {
            return applicationProperties;
        }
    }


    private static final ApplicationPropertyHolder APPLICATION_PROPERTY_HOLDER = new ApplicationPropertyHolder();


    public String buildScheduleStopIdleResourcesRestEndpoint() {
        Properties properties = APPLICATION_PROPERTY_HOLDER.getApplicationProperties();

        String deploymentTarget = properties.getProperty("deployment.target");
        if (deploymentTarget != null) {

            String provisioningServiceBaseUrl =
                    properties.getProperty(String.format("provisioningService.base.url.%s", deploymentTarget));

            if (provisioningServiceBaseUrl != null) {

                String patternEndpoint = properties.getProperty("schedule.stop.rest.endpoint");

                return MessageFormat.format(patternEndpoint, provisioningServiceBaseUrl);
            }
        }

        return null;
    }


}
