package io.ghap.userdata.health;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.commons.lang.time.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final String STATUS_FAILED = "Failed: ";

    @Override
    public Map<String, String> checkHealth() {
        Map<String, String> result = new HashMap<>();
        result.put("S3", checkS3());
        return result;
    }

    @Override
    public boolean isCheckSuccess(Map<String, String> result) {
        Collection<String> values = new ArrayList<>(result.values());
        for (String val : values) {
            if (val.startsWith(STATUS_FAILED)) {
                return false;
            }
        }
        return true;
    }

    private String checkS3() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
            AmazonS3 s3 = new AmazonS3Client(awsCredentialsProvider);
            s3.doesBucketExist("/aws/test23");
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }
}
