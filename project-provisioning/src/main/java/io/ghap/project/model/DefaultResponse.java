package io.ghap.project.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author MaximTulupov@certara.com
 */
public class DefaultResponse {

    public static final DefaultResponse SUCCESS = new DefaultResponse((Object)null);

    private boolean success;
    private Set<Error> errors;
    private Object data;

    public DefaultResponse(Object data) {
        this.data = data;
        this.success = true;
    }

    public DefaultResponse(Set<Error> errors) {
        this.errors = errors;
        this.success = false;
    }

    public DefaultResponse(Error... errors) {
        this.errors = new HashSet<>(Arrays.asList(errors));
        this.success = false;
    }


    public boolean isSuccess() {
        return success;
    }

    public Set<Error> getErrors() {
        return errors;
    }

    public Object getData() {
        return data;
    }
}
