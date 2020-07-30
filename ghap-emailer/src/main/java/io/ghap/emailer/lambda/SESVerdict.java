package io.ghap.emailer.lambda;

/**
 * Created by snagy on 12/29/15.
 */
public class SESVerdict {
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
