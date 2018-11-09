package io.ghap.data.contribution;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.annotations.Expose;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Date;
import java.util.Map;

public class Submission
{
  @Expose
  private String keyName;
  @Expose
  private String userName;
  @Expose
  private long size;
  @Expose
  private Date lastModified;
  
  public Submission() 
  {
  }

  public Submission(S3ObjectSummary summary) {
    setKeyName(summary.getKey());
    setSize(summary.getSize());
    setLastModified(summary.getLastModified());
  }

  public void setKeyName(String keyName)
  {
    this.keyName = keyName;
  }
  
  public String getKeyName()
  {
    return keyName;
  }
  
  public void setSize(long size)
  {
    this.size = size;
  }
  
  public long getSize()
  {
    return size;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }
}
