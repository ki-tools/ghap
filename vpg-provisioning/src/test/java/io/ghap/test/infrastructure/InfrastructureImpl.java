package io.ghap.test.infrastructure;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.google.inject.Singleton;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.ghap.provision.vpg.MultiVPGFactoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

//@Singleton
@LazySingleton
public class InfrastructureImpl implements Infrastructure {

  private Logger logger = LoggerFactory.getLogger(InfrastructureImpl.class);

  private String publicSubnetId;
  private String securityGroupId;
  private String stackId;

  @PostConstruct
  public void Init()
  {
    if (stackId != null) {
      //For some reason, the Init/PostConstruct was called more than once...nothing to do
      return;
    }

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    StringBuffer templateBody = new StringBuffer();
    InputStream stream = MultiVPGFactoryTest.class.getClassLoader().getResourceAsStream("test-infrastructure.template");
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

    String stackName = String.format("%s-%d", "TestInfrastructure", System.currentTimeMillis());

    CreateStackRequest createStackRequest = new CreateStackRequest();
    createStackRequest.setTemplateBody(template);
    createStackRequest.setStackName(stackName);
    createStackRequest.setDisableRollback(true);
    CreateStackResult createStackResult = cloudformationClient.createStack(createStackRequest);
    stackId = createStackResult.getStackId();

    String status = "";
    long sleep_time = 500;

    while (!status.endsWith("_COMPLETE")) {
      try {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackId);
        DescribeStacksResult describeStacksResult = cloudformationClient.describeStacks(describeStacksRequest);
        Stack stack = describeStacksResult.getStacks().get(0);
        status = stack.getStackStatus();
        if (status.endsWith("_COMPLETE")) {
          List<Output> outputs = stack.getOutputs();
          for (Output output : outputs) {
            if (output.getOutputKey().equals("GhapTestPublicSubnetOutput")) {
              publicSubnetId = output.getOutputValue();
            }
            if (output.getOutputKey().equals("GhapTestSecurityGroupOutput")) {
              securityGroupId = output.getOutputValue();
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
          continue;
        }
        logger.error("Unexpected Exception Encountered", ase);
        break; // i was not expecting this exception bailout
      }
    }

  }

  @PreDestroy
  public void CleanUp()
  {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    if (stackId != null) {
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
      deleteStackRequest.setStackName(stackId);
      cloudformationClient.deleteStack(deleteStackRequest);
    }
  }

  @Override
  public String getTestPublicSubnetId() {
    return publicSubnetId;
  }

  @Override
  public String getTestSecurityGroupId() {
    return securityGroupId;
  }
}
