package io.ghap.reporting.configuration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
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

    String bucket = System.getProperty("s3.config.bucket", ConfigurationManager.getConfigInstance().getString("s3.config.bucket"));
    if (bucket == null) {
      throw new IllegalArgumentException("\"s3.config.bucket\" system property should be specified");
    }
        /*
         * AWS credentials provider chain that looks for credentials in this order:
         * Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
         * Java System Properties - aws.accessKeyId and aws.secretKey
         * Instance profile credentials delivered through the Amazon EC2 metadata service
         */
    AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
    AmazonS3 s3 = new AmazonS3Client(awsCredentialsProvider);

    Boolean doesBucketExist = null;
    String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
    if ("dev".equals(env)) {
      try {
        doesBucketExist = s3.doesBucketExist(bucket);
      } catch (Exception e) {
        log.error("S3 integration was disabled. See logs.");
      }
    } else {
      doesBucketExist = s3.doesBucketExist(bucket);
    }

    if (doesBucketExist != null && !doesBucketExist)
      throw new AmazonClientException("Bucket " + bucket + " doesn't exist.");

    boolean isExists = false;
    for (String file : files) {
      try {
        s3.getObjectMetadata(bucket, file);
        isExists = true;
      } catch (AmazonServiceException ex) {
        if (ex.getStatusCode() == 404) {
          log.error("Cannot find configuration file \"{}\" in the \"{}\" backet", file, bucket);
        }
        if (!"dev".equals(env))
          throw ex;
      }
    }

    if (isExists) {
      ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();


      // add them in this order to make first override last
      for (String file : files) {
        S3ConfigurationSource source = new S3ConfigurationSource(s3, bucket, file);
        DynamicConfiguration configuration = new DynamicConfiguration(source, new FixedDelayPollingScheduler());
        finalConfig.addConfiguration(configuration);
      }

      if (!ConfigurationManager.isConfigurationInstalled()) {
        ConfigurationManager.install(finalConfig);
      }
    }
  }
}
