package io.ghap.project.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit {
    private String id;
    private String displayId;
    private CommitAuthor author;
    private Long authorTimestamp;
    private String message;
    private String commitDate;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public CommitAuthor getAuthor() {
        return author;
    }

    public void setAuthor(CommitAuthor author) {
        this.author = author;
    }

    public Long getAuthorTimestamp() {
        return authorTimestamp;
    }

    public void setAuthorTimestamp(Long authorTimestamp) {
        this.authorTimestamp = authorTimestamp;
    }

    public String getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(String commitDate) {
        this.commitDate = commitDate;
    }
}
