package io.ghap.web;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class PropertyTranslation {

    private final Properties dest;
    private final Map<String, ?> src;

    public PropertyTranslation(Properties dest, Map<String, ?> src){
        this.dest = dest;
        this.src = src;
    }

    public PropertyTranslation translate(String destinationName, String... sourceName){

        Object subset = src;
        for(String key:sourceName){
            if( subset instanceof Map ) {
                subset = ((Map)subset).get(key);
            } else {
                throw new IllegalArgumentException("Cannot read " + Arrays.asList(sourceName) + " property from " + src);
            }
        }

        if(subset != null){
            dest.setProperty(destinationName, String.valueOf(subset));
        }
        return this;
    }
}
