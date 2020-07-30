package io.ghap.emailer.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.*;

@DynamoDBTable(tableName = "EmailMapping")
public class EmailMapping {
  @DynamoDBHashKey(attributeName="inbound-email-addr")
  private String inboundEmailAddress;

  @DynamoDBAttribute(attributeName="outbound-email-addr")
  private Set<String> outboundEmailAddresses;

  public void setInboundEmailAddress(String emailAddress) {
    inboundEmailAddress = emailAddress;
  }

  public String getInboundEmailAddress() {
    return inboundEmailAddress;
  }

  public void setOutboundEmailAddresses(String... emailAddresses) {
    outboundEmailAddresses = new LinkedHashSet<>(Arrays.asList(emailAddresses));
  }

  public void setOutboundEmailAddresses(Set<String> emailAddresses) {
    outboundEmailAddresses = emailAddresses;
  }
  public Set<String> getOutboundEmailAddresses() {
    return outboundEmailAddresses;
  }

  public void addOutboundEmailAddress(String emailAddress) {
    if(outboundEmailAddresses == null) {
      outboundEmailAddresses = new LinkedHashSet<>();
    }
    outboundEmailAddresses.add(emailAddress);
  }

  public void removeOutboundEmailAddress(String emailAddress) {
    if(outboundEmailAddresses != null) {
      outboundEmailAddresses.remove(emailAddress);
    }
  }
}