package io.ghap.emailer.lambda;

import java.util.List;

public class CommonHeaders {
  private String returnPath;
  private String date;
  private String messageId;
  private String subject;
  private List<String> from;
  private List<String> to;

  public String getReturnPath() {
    return returnPath;
  }

  public void setReturnPath(String returnPath) {
    this.returnPath = returnPath;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public List<String> getFrom() {
    return from;
  }

  public void setFrom(List<String> from) {
    this.from = from;
  }

  public List<String> getTo() {
    return to;
  }

  public void setTo(List<String> to) {
    this.to = to;
  }
}
