package io.ghap.emailer.lambda;

import java.util.List;

public class SESMail {
  private List<String> destination;
  private String messageId;
  private String source;
  private String timestamp;
  private List<SESHeader> headers;

  public CommonHeaders getCommonHeaders() {
    return commonHeaders;
  }

  public void setCommonHeaders(CommonHeaders commonHeaders) {
    this.commonHeaders = commonHeaders;
  }

  private CommonHeaders commonHeaders;
  private String headersTruncated;

  public List<String> getDestination() {
    return destination;
  }

  public void setDestination(List<String> destination) {
    this.destination = destination;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public List<SESHeader> getHeaders() {
    return headers;
  }

  public void setHeaders(List<SESHeader> headers) {
    this.headers = headers;
  }

  public String getHeadersTruncated() {
    return headersTruncated;
  }

  public void setHeadersTruncated(String headersTruncated) {
    this.headersTruncated = headersTruncated;
  }
}
