package io.ghap.user.model;

import io.ghap.user.model.validation.OnCreate;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

//import org.apache.bval.constraints.NotEmpty;

/**
 *
 */
public class Group extends AbstractModel {


    @NotNull(groups = OnCreate.class)
    @NotEmpty(groups = OnCreate.class)
    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String name;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String description;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String info;

    public Group(){
        super("group");
    }

    public Group(String dn){
        super("group");
        setDn(dn);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "Group [dn=" + getDn() + ", name="
                + name + ", description=" + description + ", info=" + info + "]";
    }

    public static int compareByName(Group a, Group b) {
        return a.getName().compareTo(b.getName());
    }
}
