package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonTypeIdResolver;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonTypeIdResolver(ConstraintTypeIdResolver.class)
public abstract class Constraint<T> {
  public enum ConstraintType {
    DATE_RANGE,
    EXCLUDE_DATA_CURATOR;

    public static ConstraintType fromName(String name) {
      for (ConstraintType type : values()) {
        if (type.name().equals(name)) {
          return type;
        }
      }
      return null;
    }
  }
  private ConstraintType type;
  private T constraint;

  public Constraint() {}
  public void setConstraint(T constraint) {
    this.constraint = constraint;
  }
  public T getConstraint() {
    return constraint;
  }

  public void setType(ConstraintType type) {
    this.type = type;
  }

  public ConstraintType getType() {
    return type;
  }

  public abstract boolean isSatisfied();
}
