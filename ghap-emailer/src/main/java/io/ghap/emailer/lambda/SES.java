package io.ghap.emailer.lambda;

/**
 * Created by snagy on 12/29/15.
 */
public class SES {
  private SESReceipt receipt;
  private SESMail mail;
  private String notificationType;
  private String content;

  public SESReceipt getReceipt() {
    return receipt;
  }

  public void setReceipt(SESReceipt receipt) {
    this.receipt = receipt;
  }

  public SESMail getMail() {
    return mail;
  }

  public void setMail(SESMail mail) {
    this.mail = mail;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public void setNotificationType(String notificationType) {
    this.notificationType = notificationType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
