package io.ghap.auth.authorize;

import org.apache.wiki.auth.authorize.Group;

import java.security.Principal;
import java.util.Objects;

public class GhapGroup implements Principal, java.io.Serializable {
    private final String name;

    public GhapGroup(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GhapGroup)) return false;

        GhapGroup that = (GhapGroup) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
