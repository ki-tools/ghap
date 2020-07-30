package io.ghap.user.manager.tracking;


import io.ghap.user.form.UserFormData;
import io.ghap.user.model.User;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserChanges {

    private static final List<String> FIELDS = Arrays.asList("firstName", "lastName", "name", "email", "password");

    public static List<Change> check(User user, UserFormData data){
        List<Change> changes = new ArrayList(FIELDS.size());

        for(String field:FIELDS){
            Change change = compare(user, data, field);
            if(change != null)
                changes.add(change);
        }

        return changes.isEmpty() ? null:changes;
    }

    public static List<Change> check(User user, User data){
        List<Change> changes = new ArrayList(FIELDS.size());

        for(String field:FIELDS){
            Change change = compare(user, data, field);
            if(change != null)
                changes.add(change);
        }

        return changes.isEmpty() ? null:changes;
    }

    private static Change compare(Object object, Object formData, String field) {
        Object oldValue = getProperty(object, field);
        Object newValue = getProperty(formData, field);
        return (newValue == null || newValue.equals(oldValue)) ? null : new Change(field, oldValue, newValue);
    }

    private static Object getProperty(Object object, String field){
        try {
            return PropertyUtils.getProperty(object, field);
        } catch (IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot get property \"" + field + "\" for: " + object, e);
        }
    }
}
