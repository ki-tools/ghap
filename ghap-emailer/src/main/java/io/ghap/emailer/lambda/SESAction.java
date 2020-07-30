package io.ghap.emailer.lambda;

/**
 * Created by snagy on 12/29/15.
 */
public class SESAction {
  private String type;
  private String topicArn;
  private String bucketName;
  private String objectKey;
  private String smtpReplyCode;
  private String statusCode;
  private String message;
  private String sender;
  private String functionArn;
  private String invocationType;
  private String organizationArn;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTopicArn() {
    return topicArn;
  }

  public void setTopicArn(String topicArn) {
    this.topicArn = topicArn;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }

  public String getSmtpReplyCode() {
    return smtpReplyCode;
  }

  public void setSmtpReplyCode(String smtpReplyCode) {
    this.smtpReplyCode = smtpReplyCode;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getFunctionArn() {
    return functionArn;
  }

  public void setFunctionArn(String functionArn) {
    this.functionArn = functionArn;
  }

  public String getInvocationType() {
    return invocationType;
  }

  public void setInvocationType(String invocationType) {
    this.invocationType = invocationType;
  }

  public String getOrganizationArn() {
    return organizationArn;
  }

  public void setOrganizationArn(String organizationArn) {
    this.organizationArn = organizationArn;
  }
}
