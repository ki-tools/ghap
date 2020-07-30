package io.ghap.project.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.ghap.project.model.*;
import io.ghap.project.model.Error;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 */
@Singleton
public class StashExceptionService {

    @Inject
    @Named("defaultMessages")
    private ResourceBundle messages;

    @Inject
    @Named("defaultMessageCodes")
    private ResourceBundle codes;

    public Set<Error> toWebErrors(StashException ex) {
        if (CollectionUtils.isEmpty(ex.getErrors())) {
            return new HashSet<>();
        }
        Set<Error> result = new HashSet<>();
        for (StashError e : ex.getErrors()) {
            result.add(new Error(Integer.valueOf(codes.getString("stash.api.error")), e.getMessage()));
        }
        return result;
    }
}
