package io.ghap.emailer.lambda;

import com.google.gson.annotations.SerializedName;

public class SESEvent {
  @SerializedName("Records")
  private SESRecord[] records;

  public SESRecord[] getRecords() {
    return records;
  }

  public void setRecords(SESRecord[] records) {
    this.records = records;
  }
}