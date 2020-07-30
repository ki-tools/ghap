package io.ghap.reporting.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.UUID;

//TODO do not hardcode table
//reporting.dynamodb.table.name
@DynamoDBTable(tableName = "GhapReporting")
public class Report implements Comparable<Report> {

  @DynamoDBHashKey
  private String token;

  @DynamoDBAttribute
  private UUID owner;

  @DynamoDBAttribute
  private long created;

  @DynamoDBAttribute
  private String name;

  @DynamoDBAttribute
  @DynamoDBMarshalling(marshallerClass = ReportTypeMarshaller.class)
  private ReportType reportType;

  @DynamoDBAttribute
  private String contentType;

  @DynamoDBAttribute
  @JsonIgnore
  private byte[] content;

  @DynamoDBAttribute
  @JsonIgnore
  private String parentToken;

  @DynamoDBAttribute
  @JsonIgnore
  private Integer order = Integer.MAX_VALUE;

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  private String filename;

  public ReportType getReportType() {
    return reportType;
  }

  public void setReportType(ReportType reportType) {
    this.reportType = reportType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public long getCreated() {
    return created;
  }

  public void setCreated(long created) {
    this.created = created;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }

  public String getParentToken() {
    return parentToken;
  }

  public void setParentToken(String parentToken) {
    this.parentToken = parentToken;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public long calculateSize() throws IllegalAccessException {
    Field[] declaredFields = getClass().getDeclaredFields();
    long size = 0;
    for (Field field : declaredFields) {
      size += field.getName().getBytes().length;
      Object o = field.get(this);
      if (o == null) {
        continue;
      }
      if (o instanceof Enum) {
        size += ((Enum) o).name().getBytes().length;
      } else if (o instanceof byte[]) {
        size += ((byte[]) o).length;
      } else if (o instanceof Long) {
        size += 8;
      }else if (o instanceof Integer) {
        size += 4;
      } else {
        size += o.toString().getBytes().length;
      }
    }
    return size;
  }

  @Override
  public int compareTo(Report o) {
    return order.compareTo(o.getOrder());
  }
}
