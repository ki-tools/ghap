package io.ghap.session.monitor.lambda;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

class RequestMessageHelper {

    public CloudWatchMessage getCloudWatchMessage(SNSEvent.SNSRecord record) {
        Gson gson = new Gson();

        SNSEvent.SNS sns = record.getSNS();
        CloudWatchMessage message = gson.fromJson(sns.getMessage(), CloudWatchMessage.class);
        message.MessageId = sns.getMessageId();
        message.MessageTime = sns.getTimestamp();

        return message;
    }

    public String findInstanceIdAssociatedToCloudWatchMessage(CloudWatchMessage cloudWatchMessage) {
        for (Dimension dimension : cloudWatchMessage.Trigger.Dimensions) {
            if (dimension.name.equals("InstanceId")) {
                return dimension.value;
            }
        }

        return null;
    }

    public String findStackIdAssociatedWithInstance(String instanceId) {
        AmazonEC2Client ec2Client = new AmazonEC2Client();

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(Arrays.asList(instanceId));


        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);

        if (!describeInstancesResult.getReservations().isEmpty()) {
            Reservation reservation = describeInstancesResult.getReservations().get(0);
            return getStackId(reservation, instanceId);
        }

        return null;
    }

    private String getStackId(Reservation reservation, String instanceId) {
        for (Instance instance : reservation.getInstances()) {
            if (instance.getInstanceId().equals(instanceId)) {
                return getStackId(instance);
            }
        }
        return null;
    }

    private String getStackId(Instance ec2Instance) {
        List<Tag> tags = ec2Instance.getTags();
        for (Tag tag : tags) {
            if (tag.getKey().equals("aws:cloudformation:stack-id")) {
                return tag.getValue();
            }
        }
        return null;
    }
}
