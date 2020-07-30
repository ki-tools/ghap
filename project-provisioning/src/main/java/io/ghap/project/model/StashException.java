package io.ghap.project.model;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StashException extends Exception {

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    private Integer code;

    private Set<StashError> errors;

    public Set<StashError> getErrors() {
        return errors;
    }

    public void setErrors(Set<StashError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "StashException{" +
                "code=" + code +
                ", message=" + getMessage() + ", errors=" + errors +
                '}';
    }
}
