package io.ghap.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShinyAppDescriptor {

    private String path;
    private ShinyApp application;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ShinyApp getApplication() {
        return application;
    }

    public void setApplication(ShinyApp application) {
        this.application = application;
    }

    @Override
    public String toString() {
        return "ShinyAppDescriptor{" +
                "path='" + path + '\'' +
                ", application=" + application +
                '}';
    }
}
