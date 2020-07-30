package io.ghap.user.model;


import io.ghap.user.dao.ValidationError;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractModel implements Serializable, Cloneable {

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String dn;

    private final String objectClass;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String guid;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
    private List<ValidationError> errors = new ArrayList(0);

    private AbstractModel(){
        // to prevent the following error we need default constructor without arguments:
        // c.s.j.s.w.g.WadlGeneratorJAXBGrammarGenerator - Failed to generate the schema for the JAX-B elements
        // com.sun.xml.bind.v2.runtime.IllegalAnnotationsException: 1 counts of IllegalAnnotationExceptions
        throw new UnsupportedOperationException();
    }

    protected AbstractModel(String objectClass){
        this.objectClass = objectClass;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void addError(final String field, final String code, final String message){
        for(ValidationError error:errors){
            if( error.getField().equalsIgnoreCase(field) ){
                error.getErrors().add(error.new Error(code, message));
                return;
            }
        }

        errors.add(new ValidationError(field, code, message));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractModel)) return false;

        AbstractModel that = (AbstractModel) o;

        return !(dn != null ? !dn.equals(that.dn) : that.dn != null);

    }

    @Override
    public int hashCode() {
        return dn != null ? dn.hashCode() : 0;
    }
}
