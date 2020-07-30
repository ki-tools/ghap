package io.ghap.user.manager.tracking;

public class Change {
    private final String field;
    private final Object oldValue;
    private final Object newValue;
    private final boolean hide;

    public Change(final String field, final Object oldValue, final Object newValue, final boolean hide){
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.hide = hide;
    }

    public Change(final String field, final Object oldValue, final Object newValue){
        this(field, oldValue, newValue, false);
    }

    public String getField() {
        return field;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public boolean isHide() {
        return hide;
    }
}
