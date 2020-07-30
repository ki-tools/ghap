package io.ghap.reporting.data;

public class DateRangeConstraint extends Constraint<DateRange> {

  public DateRangeConstraint() {
    setType(ConstraintType.DATE_RANGE);
  }

  public boolean isSatisfied() {
    DateRange dateRange = getConstraint();
    if(dateRange.getStart() != null && dateRange.getEnd() != null) {
      if(dateRange.getStartTime() > 0 && dateRange.getEndTime() > 0) {
        return true;
      }
    }
    return false;
  }
}
