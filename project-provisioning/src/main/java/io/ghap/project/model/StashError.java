package io.ghap.project.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StashError {
    private String message;
    private String exceptionName;

    public String getMessage() {
        return message.replaceAll("(?i)project", "program").replaceAll("(?i)repository", "grant");
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }
}
