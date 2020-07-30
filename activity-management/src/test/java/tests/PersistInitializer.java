package tests;

import com.google.inject.Inject;
import com.google.inject.persist.PersistService;

public class PersistInitializer {

    @Inject
    PersistInitializer(PersistService service) {
        service.start();
        // At this point JPA is started and ready.
    }
}
