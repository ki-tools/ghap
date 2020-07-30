package io.ghap.provision.vpg;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.netflix.config.ConfigurationManager;
import io.ghap.provision.vpg.data.VirtualResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Utils {
    private static final String COMPUTE_NODE_INDICATORS = "ComputeNode,LinuxVpgGroup";//TODO - add as property in configuration
    private static final String LOGICAL_ID_KEY_NAME = "aws:cloudformation:logical-id";
    private static Logger logger = LoggerFactory.getLogger(Utils.class);


    public static String getStackId(Instance ec2Instance) {
        List<Tag> tags = ec2Instance.getTags();
        for (com.amazonaws.services.ec2.model.Tag tag : tags) {
            if (tag.getKey().equals("aws:cloudformation:stack-id")) {
                return tag.getValue();
            }
        }
        return null;
    }

    public static boolean isTopLevelNode(Instance ec2Instance) {
        List<Tag> tags = ec2Instance.getTags();
        String[] computeNodeIndicators = COMPUTE_NODE_INDICATORS.split(",");
        for (com.amazonaws.services.ec2.model.Tag tag : tags) {
            if (tag.getKey().equals(LOGICAL_ID_KEY_NAME)) {
                String tagValue = tag.getValue();
                for(String indicator : computeNodeIndicators ) {
                    if (tagValue.contains(indicator))
                        return false;
                }
            }
        }
        return true;
    }

    public static String getUserId(Instance ec2Instance) {
        List<Tag> tags = ec2Instance.getTags();
        for (com.amazonaws.services.ec2.model.Tag tag : tags) {
            if (tag.getKey().equals("UUID")) {
                return tag.getValue();
            }
        }
        return null;
    }

    public static  String getDetailedWindowsStatus(String instanceId, AmazonEC2Client ec2Client) {
        String console = console(instanceId, ec2Client);
        if (!console.toLowerCase().contains("windows is ready to use")) {
            return "initializing";
        }
        return null;
    }

    public static String console(String ec2InstanceId, AmazonEC2Client ec2Client) {
        GetConsoleOutputRequest getConsoleOutputRequest = new GetConsoleOutputRequest();
        getConsoleOutputRequest.setInstanceId(ec2InstanceId);

        GetConsoleOutputResult getConsoleOutputResult = ec2Client.getConsoleOutput(getConsoleOutputRequest);
        String consoleOutput = "";
        if (getConsoleOutputResult.getOutput() != null) {
            consoleOutput = getConsoleOutputResult.getDecodedOutput();
        }

        return consoleOutput;
    }

    public static VirtualResource getVirtualResource(VirtualPrivateGrid vpg, Instance instance,
                                               AmazonEC2Client ec2Client) {
        long start = Calendar.getInstance().getTimeInMillis();
        int cores = ConfigurationManager.getConfigInstance().getInt(instance.getInstanceType());
        VirtualResource resource = new VirtualResource();
        resource.setVpgId(vpg.getId());
        resource.setStackId(vpg.getStackId());

        resource.setAutoScaleDesiredInstanceCount(vpg.getAutoScaleDesiredInstanceCount());
        resource.setAutoScaleMaxInstanceCount(vpg.getAutoScaleMaxInstanceCount());

        resource.setCoreCount(cores);
        resource.setInstanceId(instance.getInstanceId());
        resource.setAddress(instance.getPublicIpAddress());
        resource.setDnsname(instance.getPublicDnsName());
        resource.setLaunchTime(instance.getLaunchTime());
        resource.setImageId(instance.getImageId());
        resource.setUserId(vpg.getUserId());
        resource.setInstanceOsType(instance.getPlatform() == null ? "Linux" : instance.getPlatform());
        resource.setIsTopLevelNode(Utils.isTopLevelNode(instance));

        String instanceState = instance.getState().getName();
        resource.setStatus(instanceState);
        if (instanceState.equalsIgnoreCase(InstanceStateName.Running.name())) {
            String detailedStatus = null;
            if (resource.getInstanceOsType().equalsIgnoreCase("windows")) {
                detailedStatus = getDetailedWindowsStatus(instance.getInstanceId(), ec2Client);
            } else {
                detailedStatus = getDetailedStatus(instance.getInstanceId(), ec2Client);
            }
            if (detailedStatus != null) {
                resource.setStatus(detailedStatus);
            }
        }
        long end = Calendar.getInstance().getTimeInMillis();
        if(logger.isDebugEnabled()) {
            logger.debug(String.format("getVirtualResources took %.3f seconds.", getSeconds(start,end)));
        }
        return resource;
    }

    public static double getSeconds(long start, long end) {
        double duration = ((double)end - (double)start)/1000d;
        return duration;
    }

    private static String getDetailedStatus(String instanceId, AmazonEC2Client ec2Client) {
        DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();
        describeInstanceStatusRequest.setInstanceIds(Arrays.asList(new String[]{instanceId}));
        DescribeInstanceStatusResult describeInstanceStatusResult = ec2Client.describeInstanceStatus(describeInstanceStatusRequest);

        List<InstanceStatus> instanceStatuses = describeInstanceStatusResult.getInstanceStatuses();
        InstanceStatusSummary systemStatus = instanceStatuses.get(0).getSystemStatus();
        InstanceStatusSummary instanceStatus = instanceStatuses.get(0).getInstanceStatus();
        if (!instanceStatus.getStatus().equalsIgnoreCase("ok")) {
            return "initializing";
        }

        return null;
    }
}
