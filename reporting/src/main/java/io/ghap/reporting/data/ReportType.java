package io.ghap.reporting.data;

public enum ReportType {
  USER_STATUS("User Accounts", ReportCategory.AUDITING),
  GROUP_STATUS("Group Membership", ReportCategory.AUDITING),
  ROLE_STATUS("Role Membership", ReportCategory.AUDITING),
  PROGRAM_STATUS("Programs", ReportCategory.AUDITING),
  GRANT_STATUS("Grants", ReportCategory.AUDITING),
  COMPUTE("Compute Environments", ReportCategory.USAGE, new Constraint.ConstraintType[] { Constraint.ConstraintType.DATE_RANGE }),
  WINDOWS_COMPUTE("Windows Compute Environments", ReportCategory.USAGE, new Constraint.ConstraintType[] { Constraint.ConstraintType.DATE_RANGE }),
  DATASUBMISSION("Dataset Download Log", ReportCategory.AUDITING, new Constraint.ConstraintType[] { Constraint.ConstraintType.DATE_RANGE }),
  GRANT_PERMISSION_MISMATCH("Program/Grant Assignments", ReportCategory.AUDITING);

  private String typeName;
  private ReportCategory reportCategory;
  private Constraint.ConstraintType[] constraintTypes;

  ReportType(String name, ReportCategory category, Constraint.ConstraintType... constraintTypes) {
    this.typeName = name;
    this.reportCategory = category;
    this.constraintTypes = constraintTypes;
  }

  public String getTypeName() {
    return typeName;
  }

  public ReportCategory getReportCategory() {
    return reportCategory;
  }

  public Constraint.ConstraintType[] getConstraintTypes() {
    return constraintTypes;
  }

  public static final ReportType fromName(String name) {
    for (ReportType reportType : ReportType.values()) {
      if (reportType.name().equals(name)) {
        return reportType;
      }
    }
    return null;
  }
}
