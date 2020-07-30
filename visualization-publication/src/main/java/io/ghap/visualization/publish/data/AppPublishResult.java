package io.ghap.visualization.publish.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import io.ghap.visualization.publish.data.validation.ValidationError;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.ws.rs.core.Response;

public class AppPublishResult {

    @JsonIgnore
    private Response.Status status;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
    private String message;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
    private List<ValidationError> errors;

    public AppPublishResult(Response.Status state, String message) {
        super();
        this.status = state;
        this.message = message;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        //errors = new ArrayList<>();
        //errors.add(new ValidationError("ffff", "cccc", "mmm"));
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
