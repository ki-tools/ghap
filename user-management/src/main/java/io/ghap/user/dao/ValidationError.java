package io.ghap.user.dao;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ValidationError {

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
    private final String field;

    private final List<Error> errors = new ArrayList(1);

    public ValidationError(String field, String code, String message) {
        this.field = field;
        errors.add(new Error(code, message));
    }

    public String getField() {
        return field;
    }

    public List<Error> getErrors() {
        return errors;
    }


    public class Error {
        private final String code;
        private final String message;

        public Error(final String code, final String message){
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
