package io.ghap.emailer.lambda;

import java.util.List;

public class SESReceipt {
  private SESAction action;
  private SESVerdict dkimVerdict;
  private SESVerdict spamVerdict;
  private SESVerdict spfVerdict;
  private SESVerdict virusVerdict;
  private String timestamp;
  private String processingTimeMillis;
  private List<String> recipients;

  public SESAction getAction() {
    return action;
  }

  public void setAction(SESAction action) {
    this.action = action;
  }

  public SESVerdict getDkimVerdict() {
    return dkimVerdict;
  }

  public void setDkimVerdict(SESVerdict dkimVerdict) {
    this.dkimVerdict = dkimVerdict;
  }

  public SESVerdict getSpamVerdict() {
    return spamVerdict;
  }

  public void setSpamVerdict(SESVerdict spamVerdict) {
    this.spamVerdict = spamVerdict;
  }

  public SESVerdict getSpfVerdict() {
    return spfVerdict;
  }

  public void setSpfVerdict(SESVerdict spfVerdict) {
    this.spfVerdict = spfVerdict;
  }

  public SESVerdict getVirusVerdict() {
    return virusVerdict;
  }

  public void setVirusVerdict(SESVerdict virusVerdict) {
    this.virusVerdict = virusVerdict;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getProcessingTimeMillis() {
    return processingTimeMillis;
  }

  public void setProcessingTimeMillis(String processingTimeMillis) {
    this.processingTimeMillis = processingTimeMillis;
  }

  public List<String> getRecipients() {
    return recipients;
  }

  public void setRecipients(List<String> recipients) {
    this.recipients = recipients;
  }
}
