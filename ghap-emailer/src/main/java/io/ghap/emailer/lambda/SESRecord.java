package io.ghap.emailer.lambda;

public class SESRecord {
  private String eventVersion;
  private SES ses;
  private String eventSource;

  public String getEventVersion() {
    return eventVersion;
  }

  public void setEventVersion(String eventVersion) {
    this.eventVersion = eventVersion;
  }

  public SES getSES() {
    return ses;
  }

  public void setSES(SES ses) {
    this.ses = ses;
  }

  public String getEventSource() {
    return eventSource;
  }

  public void setEventSource(String eventSource) {
    this.eventSource = eventSource;
  }
}
