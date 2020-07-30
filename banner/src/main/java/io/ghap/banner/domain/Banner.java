package io.ghap.banner.domain;

import io.ghap.banner.model.ApiBanner;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;


@Entity
@Table(name = "BANNER")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Banner extends BaseDBEntityImpl implements ApiBanner {
    private String title;
    private String message;
    private Date startDate;
    private Date endDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
