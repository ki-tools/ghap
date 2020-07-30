package io.ghap.provision.health;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.lang.time.StopWatch;
import org.quartz.Scheduler;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final String STATUS_FAILED = "Failed: ";

    @Inject
    private Provider<EntityManager> entityManagerProvider;

    @Inject
    private Provider<Scheduler> schedulerProvider;

    @Override
    public Map<String, String> checkHealth() {
        Map<String, String> result = new HashMap<>();
        result.put("db", pingDb());
        result.put("EC2", checkEc2());
        result.put("Quartz", checkScheduler());
        result.put("Cloud Formation", checkCloudFormation());
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

    private String pingDb() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Query query = entityManagerProvider.get().createNativeQuery("select 1");
            query.getSingleResult();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private String checkEc2() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
            AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());
            ec2Client.describeInstanceStatus();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private String checkCloudFormation() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
            AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
            cloudformationClient.listStacks(new ListStacksRequest().withStackStatusFilters(StackStatus.DELETE_IN_PROGRESS));
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private String checkScheduler() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            schedulerProvider.get().getMetaData();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }
}
