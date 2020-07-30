package io.ghap.project.domain;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public enum ObjectClass {

    user,
    group;

    public String getName() {
        return name();
    }

    public String getStashTarget() {
        return getName() + "s";
    }

    public static ObjectClass fromString(String val) {
        if (val == null) {
            return null;
        }
        for (ObjectClass oc : ObjectClass.values()) {
            if (val.equalsIgnoreCase(oc.getName())) {
                return oc;
            }
        }
        return null;
    }
}
