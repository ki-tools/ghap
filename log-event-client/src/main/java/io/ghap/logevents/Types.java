package io.ghap.logevents;

public enum Types {
    DATASET_DOWNLOAD, USER_WORKSPACE_DOWNLOAD, SHINY_APPS_SEARCH;

    public String toType() {
        return name().toLowerCase().replace('_', '-');
    }
}
