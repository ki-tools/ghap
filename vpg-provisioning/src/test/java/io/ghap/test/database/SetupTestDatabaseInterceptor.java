package io.ghap.test.database;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.cloudformation.model.Stack;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.netflix.config.ConfigurationManager;
import io.ghap.Injectable;
import io.ghap.test.infrastructure.Infrastructure;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class SetupTestDatabaseInterceptor implements MethodInterceptor {

  private static final int MAX_TRIES = 3;
  private static final int port = ConfigurationManager.getConfigInstance().getInt("vpg-provisioning.db.port");

  private static final String DB = ConfigurationManager.getConfigInstance().getString("vpg-provisioning.db.dbName");

  private static final String username = ConfigurationManager.getConfigInstance().getString("vpg-provisioning.db.user");

  private static final String password = ConfigurationManager.getConfigInstance().getString("vpg-provisioning.db.password");

  private Logger logger = LoggerFactory.getLogger(SetupTestDatabaseInterceptor.class);

  private static long TIMEOUT = 1000 * 60 * 15; // 5 minutes

  private Map<String,String> stackMap = new HashMap<>();
  private Map<String,String> ipMap = new HashMap<>();

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    SetupTestDatabase setupDbAnnotation = methodInvocation.getMethod().getAnnotation(SetupTestDatabase.class);
    if(setupDbAnnotation != null) {
      String stackPrefix = setupDbAnnotation.stackPrefix();
      long stackSuffix = System.currentTimeMillis();

      Object obj = methodInvocation.getThis();
      if(obj instanceof Injectable) {
        Injectable injectable = (Injectable)obj;
        Injector injector = injectable.whichInjector();
        Infrastructure infrastructure = injector.getInstance(Infrastructure.class);
        if(infrastructure == null) { throw new IllegalStateException("No Infrastructure Class Bound"); }

        // before
        String publicSubnet = infrastructure.getTestPublicSubnetId();
        String securityGroup = infrastructure.getTestSecurityGroupId();

        for(int try_count = 0; try_count < MAX_TRIES; try_count++) {
          try {
            createStackForDatabase(stackPrefix, stackSuffix, securityGroup, publicSubnet); // do something before method invocation
            break;                                                                         // success abort the loop
          } catch (RuntimeException re) {
            if (try_count == (MAX_TRIES - 1)) {
              throw re;
            } else {
              System.err.println("Failed to Create Test Stack Retrying.");
            }
          }
        }

        String url = getJDBCConnectionString(ipMap.get(stackPrefix));
        Properties properties = getJPAConnectionSettings(url, username, password);
        JpaPersistModule jpaModule = new JpaPersistModule(setupDbAnnotation.persistenceUnit()).properties(properties);
        Injector childInjector = injector.createChildInjector(jpaModule);
        childInjector.getInstance(PersistService.class).start();
        injectable.setChildInjector(childInjector);


        try {
          return methodInvocation.proceed();     // invoke the method

        } finally {
          afterMethodInvocation(stackPrefix);             // do something after method invocation
        }
      }
    }
    return methodInvocation.proceed();
  }

  private void createStackForDatabase(String stackPrefix, long stackSuffix, String securityGroup, String publicSubnet)
          throws IOException {

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    String template = loadTemplateContent("test-instance.template");

    String stackName = String.format("%s-%d", stackPrefix, stackSuffix);

    CreateStackRequest createStackRequest = new CreateStackRequest();
    createStackRequest.setTemplateBody(template);
    createStackRequest.setStackName(stackName);
    List<Parameter> params = new ArrayList<Parameter>();
    params.add(new Parameter().withParameterKey("GhapTestSecurityGroup").withParameterValue(securityGroup));
    params.add(new Parameter().withParameterKey("GhapTestPublicSubnet").withParameterValue(publicSubnet));
    createStackRequest.setParameters(params);
    CreateStackResult createStackResult = cloudformationClient.createStack(createStackRequest);
    String stackId = createStackResult.getStackId();


    boolean stackCreationSuccess = waitForStackCreationToComplete(cloudformationClient, stackId);
    if (stackCreationSuccess) {

      stackMap.put(stackPrefix, stackId);
      String publicip = getPublicIpAddress(cloudformationClient, stackId);
      ipMap.put(stackPrefix, publicip);

    } else {
      throw new RuntimeException("Failed to create Cloudformation stack for Test Database");
    }

  }

  private String loadTemplateContent(String templateLocation) throws IOException {
    StringBuffer templateBody = new StringBuffer();
    InputStream stream = getClass().getClassLoader().getResourceAsStream(templateLocation);
    InputStreamReader reader = new InputStreamReader(stream);
    try {
      char[] buffer = new char[1024];

      for (int x = reader.read(buffer); x > 0; x = reader.read(buffer)) {
        templateBody.append(buffer, 0, x);
      }

    } finally {
      try {
        reader.close();
      } catch (IOException ignore) {}
    }

    return templateBody.toString();
  }

  private boolean waitForStackCreationToComplete(AmazonCloudFormationClient cloudformationClient, String stackId) {

    String status = "";

    long sleep_time = 500;

    while (!status.endsWith("_COMPLETE")) {

      Stack stackInformation = getStackInformation(cloudformationClient, stackId);
      status = stackInformation.getStackStatus();

      try {
        Thread.sleep(sleep_time);
      } catch (InterruptedException ignore) { }
    }

    boolean success = status.equals("CREATE_COMPLETE");
    if(!success) {
      Stack stackInformation = getStackInformation(cloudformationClient, stackId);
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
      deleteStackRequest.setStackName(stackInformation.getStackName());
      cloudformationClient.deleteStack(deleteStackRequest);
    }
    return success ;
  }


  private String getPublicIpAddress(AmazonCloudFormationClient cloudformationClient, String stackId) {

    Stack stackInformation = getStackInformation(cloudformationClient, stackId);

    String status = stackInformation.getStackStatus();
    if (status.endsWith("CREATE_COMPLETE")) {
      List<Output> outputs = stackInformation.getOutputs();
      for (Output output : outputs) {
        if (output.getOutputKey().equals("PublicIp")) {
          return output.getOutputValue();
        }
      }
    }

    return null;
  }

  private Stack getStackInformation(AmazonCloudFormationClient cloudformationClient, String stackId) {


    long start = System.currentTimeMillis();
    long sleep_time = 1;


    while (true) {
      try {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackId);
        DescribeStacksResult describeStacksResult = cloudformationClient.describeStacks(describeStacksRequest);

        return describeStacksResult.getStacks().get(0);


      } catch (AmazonServiceException ase) {
        long durationSinceStart = System.currentTimeMillis() - start;

        if (durationSinceStart > TIMEOUT) {
          throw ase; //we have exceeded timeout, so re-throw the exception back

        } else if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
          try {
            Thread.sleep(sleep_time);
          } catch (InterruptedException ignore) {
          }
          sleep_time *= 2;

        } else {
          throw ase; // this is some other AWS exception not associated with throttling.
        }
      }
    }

  }

  private void afterMethodInvocation(String stackPrefix) {
    System.err.println(String.format("afterMethodInvocation-%s", stackPrefix));
    // after method invocation
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    if (stackPrefix != null && stackMap.containsKey(stackPrefix)) {
      System.err.println(String.format("Deleting Stack %s", stackPrefix));
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
      deleteStackRequest.setStackName(stackMap.remove(stackPrefix));
      cloudformationClient.deleteStack(deleteStackRequest);
      ipMap.remove(stackPrefix);
    }
  }

  private String getJDBCConnectionString(String hostIpAddress) {
    return String.format("jdbc:postgresql://%s:%d/%s", hostIpAddress, port, DB);
  }

  private Properties getJPAConnectionSettings(String url, String user, String pwd) {
    Properties properties = new Properties();
    properties.setProperty("javax.persistence.jdbc.url", url);
    properties.setProperty("javax.persistence.jdbc.user", user);
    properties.setProperty("javax.persistence.jdbc.password", pwd);

    return properties;
  }
}
