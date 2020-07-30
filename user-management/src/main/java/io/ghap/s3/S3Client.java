package io.ghap.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

//@Singleton
public class S3Client {

    private final String PLACEHOLDER_FILE_NAME = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("s3.bucket")
    //@NotNull
    private String bucket;// default

    @Configuration("s3.maxConnections")
    private int maxConnections = 50;

    @Configuration("s3.users.rootPath")
    private String rootPath = "users";// default

    private AmazonS3 s3;

    //@PostConstruct
    void createS3Client() {
        rootPath = correctPath(rootPath, true);
        /*
         * AWS credentials provider chain that looks for credentials in this order:
         * Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
         * Java System Properties - aws.accessKeyId and aws.secretKey
         * Instance profile credentials delivered through the Amazon EC2 metadata service
         */
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        ClientConfiguration conf = new ClientConfiguration();
        conf.setMaxConnections( maxConnections );
        s3 = new AmazonS3Client(awsCredentialsProvider, conf);

        Boolean doesBucketExist = null;
        String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
        if("dev".equals(env) || "test".equals(env) || "local".equals(env)){
            try {
                doesBucketExist = s3.doesBucketExist(bucket);
            }
            catch(Exception e){
                log.error("S3 integration was disabled. See logs.");
            }
        }
        else  {
            doesBucketExist = s3.doesBucketExist(bucket);
        }

        if(doesBucketExist != null && !doesBucketExist)
            throw new AmazonClientException("Bucket " + bucket + " doesn't exist.");
    }

    public PutObjectResult makeFolder(String path, String name) throws IOException {

        path = correctPath(path, true);
        name = correctPath(name, true);

        String key = rootPath + path + name + PLACEHOLDER_FILE_NAME;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(0);

        try(InputStream is = new ByteArrayInputStream(new byte[0])) {
            return s3.putObject(new PutObjectRequest(bucket, key, is, meta));
        }
    }

    public String correctPath(String path, boolean asFolder) {
        if (asFolder) { // only folder
            if (path == null || ".".equals(path) || "/".equals(path) || "".equals(path)) {
                path = "";
            }
            else {
                if (path.startsWith("/")) path = path.substring(1);
                if (!path.endsWith("/")) path = path + "/";
            }
        }
        else { // full relative path
            if (path == null || "/".equals(path)) path = ".";
            if (path.startsWith("/")) path = path.substring(1);
        }
        return path;
    }

}
