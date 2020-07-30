package io.ghap.visualization.publish.search;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ShinyAppItem {

    private final static DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @JsonProperty("ApplicationName")
    @SerializedName("ApplicationName")
    private String applicationName;

    @JsonProperty("SortPriority")
    @SerializedName("SortPriority")
    private long sortPriority;

    @JsonProperty("Updated")
    @SerializedName("Updated")
    private Date updated;

    @JsonProperty("Description")
    @SerializedName("Description")
    private String description;

    @JsonProperty("Keyword")
    @SerializedName("Keyword")
    private String keyword;

    @JsonProperty("Author")
    @SerializedName("Author")
    private String author;

    @JsonProperty("ApplicationRoot")
    @SerializedName("ApplicationRoot")
    private String applicationRoot;

    @JsonProperty("Thumbnail")
    @SerializedName("Thumbnail")
    private String thumbnail;

    @JsonProperty("Project")
    @SerializedName("Project")
    private List<String> project;

    @JsonProperty("Grant")
    @SerializedName("Grant")
    private List<String> grant;
    private String type;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public long getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(long sortPriority) {
        this.sortPriority = sortPriority;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getApplicationRoot() {
        return applicationRoot;
    }

    public void setApplicationRoot(String applicationRoot) {
        this.applicationRoot = applicationRoot;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<String> getProject() {
        return project;
    }

    public void setProject(List<String> project) {
        this.project = project;
    }

    public List<String> getGrant() {
        return grant;
    }

    public void setGrant(List<String> grant) {
        this.grant = grant;
    }

    public static ShinyAppItem build(Map<String, ?> map){
        Objects.requireNonNull(map, "\"map\" parameter cannot be null");
        ShinyAppItem item = new ShinyAppItem();

        item.setApplicationName((String) map.get("ApplicationName"));
        item.setApplicationRoot((String) map.get("ApplicationRoot"));
        item.setAuthor((String) map.get("Author"));
        item.setDescription((String) map.get("Description"));
        item.setType((String) map.get("Type"));
        item.setThumbnail((String) map.get("Thumbnail"));

        Object val = map.get("SortPriority");
        if(val != null) {
            item.setSortPriority(val instanceof Double ? ((Double) val).longValue() : ((Long) val).longValue());
        }

        val = map.get("Updated");
        if(val != null) {
            item.setUpdated( parseDateTime(val) );
        }

        val = map.get("Keyword");
        if(val != null) {
            item.setKeyword(val instanceof String ? (String) val : Joiner.on(',').join((Iterable<?>) val));
        }

        val = map.get("Project");
        if(val != null) {
            item.setProject(val instanceof List ? (List) val :
                            Splitter
                                    .on(CharMatcher.anyOf("@^"))
                                    .trimResults()
                                    .omitEmptyStrings()
                                    .splitToList((CharSequence) val)
            );
        }

        val = map.get("Grant");
        if(val != null) {
            item.setGrant(val instanceof List ? (List) val :
                            Splitter
                                    .on(CharMatcher.anyOf("@^"))
                                    .trimResults()
                                    .omitEmptyStrings()
                                    .splitToList((CharSequence) val)
            );
        }


        return item;
    }

    private static Date parseDateTime(Object val){
        if(val instanceof Date){
            return (Date) val;
        }
        try {
            return fmt.parse(String.valueOf(val));
        } catch (ParseException e) {
            return Date.from(OffsetDateTime.parse(String.valueOf(val)).toInstant());
        }
    }

    @Override
    public String toString() {
        return "ShinyAppItem{" +
                "applicationName='" + applicationName + '\'' +
                '}';
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
