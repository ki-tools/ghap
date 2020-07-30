package io.ghap.user.model.validation;

public interface OnUpdate {
    public <T> T validate( T o);
}
