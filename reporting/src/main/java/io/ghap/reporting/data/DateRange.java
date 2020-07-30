package io.ghap.reporting.data;

import java.util.Date;

public class DateRange {
  private Date start;
  private Date end;

  public DateRange() {}

  public DateRange(Date start, Date end) {
    setStart(start);
    setEnd(end);
  }

  public DateRange(long start, long end) {
    setStart(new Date(start));
    setEnd(new Date(end));
  }

  public long getEndTime() {
    return end.getTime();
  }
  public Date getEnd() { return end; }

  public void setEnd(Date end) {
    this.end = end;
  }

  public Date getStart() {

    return start;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  public long getStartTime() { return start.getTime(); }
}
