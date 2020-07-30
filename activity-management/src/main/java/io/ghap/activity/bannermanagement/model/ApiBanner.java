package io.ghap.activity.bannermanagement.model;


import java.util.Date;
import java.util.UUID;

/**
 */
public interface ApiBanner {
    UUID getId();

    void setId(UUID id);

    String getTitle();

    void setTitle(String title);

    String getMessage();

    void setMessage(String message);

    Date getStartDate();

    void setStartDate(Date startDate);

    Date getEndDate();

    void setEndDate(Date endDate);

    String getColor();

    void setColor(String color);
}
