package io.ghap.project.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StashLinks {

    private List<StashLink> clone;
    private List<StashLink> self;

    public List<StashLink> getClone() {
        return clone;
    }

    public void setClone(List<StashLink> clone) {
        this.clone = clone;
    }

    public List<StashLink> getSelf() {
        return self;
    }

    public void setSelf(List<StashLink> self) {
        this.self = self;
    }
}
