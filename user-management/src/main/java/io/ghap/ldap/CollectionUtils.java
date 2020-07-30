package io.ghap.ldap;

import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Domain;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;

import java.util.Collections;
import java.util.List;

/**
 */
public class CollectionUtils {

    public static final void sortUsers(List<User> users) {
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(users)) {
            Collections.sort(users, User::compareByFullName);
        }
    }

    public static final void sortGroups(List<Group> groups) {
        if (!org.apache.commons.collections.CollectionUtils.isEmpty(groups)) {
            Collections.sort(groups, Group::compareByName);
        }
    }

    public static final void sort(List<AbstractModel> abstractModels) {
        Collections.sort(abstractModels, (AbstractModel model1, AbstractModel model2) -> {
            return getComparableValue(model1).compareTo(getComparableValue(model2));
        });
    }

    private static final String getComparableValue(AbstractModel abstractModel) {
        if (abstractModel == null) {
            return null;
        }
        if (abstractModel instanceof User) {
            return ((User) abstractModel).getFullName();
        }
        if (abstractModel instanceof Group) {
            return ((Group) abstractModel).getName();
        }
        if (abstractModel instanceof Domain) {
            return ((Domain) abstractModel).getName();
        }
        return null;
    }
}
