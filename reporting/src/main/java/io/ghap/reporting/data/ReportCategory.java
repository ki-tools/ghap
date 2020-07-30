package io.ghap.reporting.data;

public enum ReportCategory {
  USER("User"),
  USAGE("Usage"),
  AUDITING("Auditing"),
  COST_MANAGEMENT("Cost Management");

  private String categoryName;

  ReportCategory(String name) {
    this.categoryName = name;
  }

  public String getCategoryName() {
    return categoryName;
  }
}
