package io.ghap.web;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.netflix.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

//@Singleton
public class S3Client {

    private static S3Client instance;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String configBucket;


    private final AmazonS3 s3Config;

    public static S3Client getInstance(){
        if(instance == null){
            synchronized (S3Client.class){
                if(instance == null){
                    instance = new S3Client();
                }
            }
        }
        return instance;
    }

    private S3Client() {
        this.configBucket = Objects.requireNonNull( getConfigurationBucket(), "Cannot define configuration configBucket name" );

        /*
         * AWS credentials provider chain that looks for credentials in this order:
         * Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
         * Java System Properties - aws.accessKeyId and aws.secretKey
         * Instance profile credentials delivered through the Amazon EC2 metadata service
         */
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        ClientConfiguration conf = new ClientConfiguration();
        s3Config = new AmazonS3Client(awsCredentialsProvider, conf);

        Boolean doesBucketExist = null;
        String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
        if("test".equals(env) || "local".equals(env) || "devtest".equals(env)){
            try {
                doesBucketExist = s3Config.doesBucketExist(configBucket);
            }
            catch(Exception e){
                log.error("S3 integration was disabled. See logs.");
            }
        }
        else  {
            doesBucketExist = s3Config.doesBucketExist(configBucket);
        }

        if(doesBucketExist != null && !doesBucketExist)
            throw new AmazonClientException("Configuration bucket " + configBucket + " doesn't exist.");
    }

    public AmazonS3 getS3Config() {
        return s3Config;
    }

    public static String getConfigurationBucket(){
        return System.getProperty("s3.config.bucket", ConfigurationManager.getConfigInstance().getString("s3.config.bucket"));
    }

    public S3ObjectInputStream getConfigFile(String key)  {
        try {
            return s3Config.getObject(new GetObjectRequest(configBucket, key)).getObjectContent();
        } catch (AmazonS3Exception ex){
            if(ex.getStatusCode() != 404){
                log.error("Cannot retrieve object \"" + key + "\" from bucket \"" + configBucket + "\". " + ex);
            }
            throw ex;
        }
    }
}
