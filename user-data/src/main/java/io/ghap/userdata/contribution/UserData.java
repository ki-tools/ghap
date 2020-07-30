package io.ghap.userdata.contribution;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Date;
import java.util.Objects;

public class UserData implements Comparable<UserData>
{
  private final String path;
  private final String name;
  private final boolean isDirectory;

  @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
  private final Date lastModified;

  @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
  private final Long size;

  public UserData(String path, String name, boolean isDirectory, Long size, Date lastModified)
  {
    this.path = path;
    this.name = Objects.requireNonNull(name);
    this.isDirectory = isDirectory;
    this.lastModified = lastModified;
    this.size = size;
  }
  
  public Long getSize()
  {
    return size;
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public Date getLastModified() {
    return lastModified;
  }

  @Override
  public int compareTo(UserData o) {
    return name.compareTo(o.name);
  }
}
