package io.ghap.model;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShinyApp {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @JsonProperty("ApplicationName")
    private String ApplicationName;
    @JsonProperty("ApplicationRoot")
    private String ApplicationRoot;
    @JsonProperty("Updated")
    @JsonDeserialize(using = DateDeserializer.class)
    private Date Updated;
    @JsonProperty("Description")
    private String Description;
    @JsonProperty("Keyword")
    private String[] Keyword;
    @JsonProperty("Grant")
    private String Grant;
    @JsonProperty("Author")
    private String Author;
    @JsonProperty("SortPriority")
    private Integer SortPriority;
    @JsonProperty("Project")
    private String Project;
    @JsonProperty("Thumbnail")
    private String Thumbnail;

    public String getApplicationName() {
        return ApplicationName;
    }

    public void setApplicationName(String applicationName) {
        this.ApplicationName = applicationName;
    }

    public String getApplicationRoot() {
        return ApplicationRoot;
    }

    public void setApplicationRoot(String applicationRoot) {
        this.ApplicationRoot = applicationRoot;
    }

    public Date getUpdated() {
        return Updated;
    }

    public void setUpdated(Date updated) {
        this.Updated = updated;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        this.Description = description;
    }

    public String[] getKeyword() {
        return Keyword;
    }

    public void setKeyword(String[] keyword) {
        this.Keyword = keyword;
    }

    public String getGrant() {
        return Grant;
    }

    public void setGrant(String grant) {
        this.Grant = grant;
    }

    public String getAuthor() {
        return Author;
    }

    public void setAuthor(String author) {
        this.Author = author;
    }

    public Integer getSortPriority() {
        return SortPriority;
    }

    public void setSortPriority(Integer sortPriority) {
        this.SortPriority = sortPriority;
    }

    public String getProject() {
        return Project;
    }

    public void setProject(String project) {
        this.Project = project;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.Thumbnail = thumbnail;
    }

    @Override
    public String toString() {
        return "ShinyApp{" +
                "ApplicationName='" + ApplicationName + '\'' +
                ", ApplicationRoot='" + ApplicationRoot + '\'' +
                ", Updated=" + Updated +
                ", Description='" + Description + '\'' +
                ", Keyword=" + Arrays.toString(Keyword) +
                ", Grant='" + Grant + '\'' +
                ", Author='" + Author + '\'' +
                ", SortPriority=" + SortPriority +
                ", Project='" + Project + '\'' +
                ", Thumbnail='" + Thumbnail + '\'' +
                '}';
    }

    private static class DateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String text = jp.getText();
            try {
                return new SimpleDateFormat(DATE_FORMAT).parse(text);
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
    }
}
