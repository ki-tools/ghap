package io.ghap.provision.vpg.data;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by arao on 1/19/16.
*/
public class StackInfoRetriever {

  private static final String STACK_PARAM_EMAIL = "Email";
  private static final String STACK_PARAM_USERNAME = "Username";
  private static final String STACK_TAG_ACTIVITY_NAME = "GhapActivityName";
  private static final String STACK_TAG_AUTOSTOP_AVAILABILITY_FLAG = "AutostopIdleResources";

  private final Map<String, String> stackInfo;

  private Logger logger = LoggerFactory.getLogger(StackInfoRetriever.class);

  public StackInfoRetriever(String stackId) {
    stackInfo = getStackInfo(stackId);
  }

  private Map<String, String> getStackInfo(String stackId) {
    Map<String, String> stackInfo = new HashMap<String, String>();

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Finding email address of user associated with stack id %s", stackId));
    }

    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient();

    DescribeStacksRequest stackRequest = new DescribeStacksRequest();
    stackRequest.setStackName(stackId);


    DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
    List<Stack> stacks = stackResults.getStacks();

    if (stacks.size() == 1) {
      Stack stack = stacks.get(0); // there can be only one

      for (Parameter stackParam : stack.getParameters()) {
        String parameterKey = stackParam.getParameterKey();
        if (parameterKey.equals(STACK_PARAM_EMAIL) || parameterKey.equals(STACK_PARAM_USERNAME)) {

          stackInfo.put(parameterKey, stackParam.getParameterValue());
        }
      }

      for (com.amazonaws.services.cloudformation.model.Tag stackTag : stack.getTags()) {
        String tagKey = stackTag.getKey();
        if (tagKey.equals(STACK_TAG_ACTIVITY_NAME) || tagKey.equals(STACK_TAG_AUTOSTOP_AVAILABILITY_FLAG)) {
          stackInfo.put(tagKey, stackTag.getValue());
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Found user information associated with stack is <%s", stackInfo));
    }

    return stackInfo;
  }

  public boolean isAutostopAvailableForStack() {
    String autostopAvailabilityFlag = stackInfo.get(STACK_TAG_AUTOSTOP_AVAILABILITY_FLAG);

    if (autostopAvailabilityFlag != null && !autostopAvailabilityFlag.trim().isEmpty()) {
      return Boolean.valueOf(autostopAvailabilityFlag);

    } else {
      return true;
    }
  }

  public String getUserName() {
    return stackInfo.get(STACK_PARAM_USERNAME);
  }

  public String getUserEmail() {
    return stackInfo.get(STACK_PARAM_EMAIL);
  }

}
