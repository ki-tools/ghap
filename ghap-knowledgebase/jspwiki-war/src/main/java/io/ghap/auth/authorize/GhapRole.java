package io.ghap.auth.authorize;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.security.Principal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GhapRole implements Principal, java.io.Serializable {
    private String name;

    public GhapRole(){

    }

    public GhapRole(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GhapRole)) return false;

        GhapRole that = (GhapRole) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
