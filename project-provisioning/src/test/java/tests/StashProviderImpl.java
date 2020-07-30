package tests;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.netflix.config.ConfigurationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

/**
 */
public class StashProviderImpl implements StashProvider {

    private boolean shouldPersistenceStoreInAWSStack = true;

    private String stackId;
    private String publicIp;

    @Override
    public String getStackId() {
        return stackId;
    }

    @Override
    public String getPublicIp() {
        return publicIp;
    }

    public void setUp() {
        if (shouldPersistenceStoreInAWSStack) {
            if (System.getProperty(SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY) == null) {
                System.setProperty(SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY, "");
            }
            if (System.getProperty(SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY) == null) {
                System.setProperty(SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY, "");
            }
            setupAWSStack();
            Properties properties = new Properties();
            properties.put("stash.url", "https://" + getPublicIp() + "/stash/rest/api/1.0");
            ConfigurationManager.loadProperties(properties);
        }
    }

    private void setupAWSStack() {
        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

        StringBuffer templateBody = new StringBuffer();
        InputStream stream = getClass().getClassLoader().getResourceAsStream("test-template.json");
        InputStreamReader reader = new InputStreamReader(stream);
        try {
            char[] buffer = new char[1024];

            for (int x = reader.read(buffer); x > 0; x = reader.read(buffer)) {
                templateBody.append(buffer, 0, x);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ioe) {
            }
        }

        String template = templateBody.substring(0);

        String stackName = String.format("TestProjectService-%d", System.currentTimeMillis());

        CreateStackRequest createStackRequest = new CreateStackRequest();
        createStackRequest.setTemplateBody(template);
        createStackRequest.setStackName(stackName);
        CreateStackResult createStackResult = cloudformationClient.createStack(createStackRequest);
        stackId = createStackResult.getStackId();

        String status = "";
        long sleep_time = 1000;

        while (!status.endsWith("_COMPLETE") && !status.startsWith("ROLLBACK") && !status.startsWith("DELETE")) {
            try {
                DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
                describeStacksRequest.setStackName(stackId);
                DescribeStacksResult describeStacksResult = cloudformationClient.describeStacks(describeStacksRequest);
                Stack stack = describeStacksResult.getStacks().get(0);
                status = stack.getStackStatus();
                if (status.endsWith("_COMPLETE")) {
                    List<Output> outputs = stack.getOutputs();
                    for (Output output : outputs) {
                        if (output.getOutputKey().equals("PublicIp")) {
                            publicIp = output.getOutputValue();
                        }
                    }
                }
                try {
                    Thread.sleep(sleep_time);
                } catch (InterruptedException ie) {
                }
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
                    sleep_time = sleep_time * 2;
                }
            }
        }
        //wait 2 minutes while stash start up
        //TODO this is a temporarily solution. Find a way to check stash status
        for (int i = 0; i < 120; i++) {
            try {
                Thread.sleep(sleep_time);
            } catch (InterruptedException ie) {
            }
        }

    }

    public void cleanUp() {
        if (shouldPersistenceStoreInAWSStack) {
            cleanUpAWSStack();
        }
    }

    private void cleanUpAWSStack() {

        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
        if (stackId != null) {
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
            deleteStackRequest.setStackName(stackId);
            cloudformationClient.deleteStack(deleteStackRequest);
        }
    }
}
