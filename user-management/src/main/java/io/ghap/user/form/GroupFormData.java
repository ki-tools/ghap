package io.ghap.user.form;

import io.ghap.user.model.Group;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupFormData {
    private String name;
    private String description;
    private String parentDn;

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

    public String getParentDn() {
        return parentDn;
    }

    public void setParentDn(String parentDn) {
        this.parentDn = parentDn;
    }

    public Group toGroup(Group group){
        if(group == null)
            group = new Group();

        if(name != null)
            group.setName(name);

        if(description != null)
            group.setDescription(description);

        return group;
    }
    public Group toGroup(){
        return toGroup(null);
    }
}
