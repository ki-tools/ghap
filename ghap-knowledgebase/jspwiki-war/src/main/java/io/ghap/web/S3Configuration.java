package io.ghap.web;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.sources.S3ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class S3Configuration {


    private static final Logger log = LoggerFactory.getLogger(S3Configuration.class);

    public static void init(String... files) {
        Objects.requireNonNull(files);

        String bucket = S3Client.getConfigurationBucket();
        AmazonS3 s3 = S3Client.getInstance().getS3Config();

        boolean isExists = false;
        for(String file:files) {
            try {
                s3.getObjectMetadata(bucket, file);
                isExists = true;
            } catch (AmazonServiceException ex) {
                if (ex.getStatusCode() == 404) {
                    log.error("Cannot find configuration file \"{}\" in the \"{}\" bucket", file, bucket);
                }

                String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
                if (!("test".equals(env) || "local".equals(env) || "devtest".equals(env)))
                    throw ex;
            }
        }

        if(isExists) {
            ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();


            // add them in this order to make first override last
            for(String file:files) {
                S3ConfigurationSource source = new S3ConfigurationSource(s3, bucket, file);
                DynamicConfiguration configuration = new DynamicConfiguration(source, new FixedDelayPollingScheduler());
                finalConfig.addConfiguration(configuration);
            }

            ConfigurationManager.install(finalConfig);
        }
    }
}
