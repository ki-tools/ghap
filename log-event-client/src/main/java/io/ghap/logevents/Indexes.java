package io.ghap.logevents;

public enum Indexes {
    GHAP, GHAP_TEST, SHINY_SEARCH, SHINY_SEARCH_TEST;

    public String toIndex() {
        return name().toLowerCase().replace('_', '-');
    }
}
