package io.ghap.reporting.data;

/**
 * This class exists solely to provide an easy transport mechansim to/from json to
 * be serialized in the {@link ReportResource.getAvailableReports} entry point.  Since
 * {@link ReportType} contain all of this information (and more) that is what we use
 * internally, but since enum's only serialize their name (without creating a custom serializer/deserializer)
 * we use this wrapper class.
 */
public class ReportDescriptor {
  private ReportType type;
  private String categoryName;
  private String typeName;
  private Constraint.ConstraintType[] constraintTypes;

  public ReportDescriptor() { super(); }
  public ReportDescriptor(ReportType reportType) {
    setType(reportType);
    setConstraintTypes(reportType.getConstraintTypes());
    setCategoryName(reportType.getReportCategory().getCategoryName());
    setTypeName(reportType.getTypeName());
  }

  public ReportType getType() {
    return type;
  }

  public void setType(ReportType type) {
    this.type = type;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public Constraint.ConstraintType[] getConstraintTypes() {
    return constraintTypes;
  }

  public void setConstraintTypes(Constraint.ConstraintType[] constraintTypes) {
    this.constraintTypes = constraintTypes;
  }
}
