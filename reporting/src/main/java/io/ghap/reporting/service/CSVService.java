package io.ghap.reporting.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface CSVService {
    void printRecords(Appendable out, List records, List<String> header, String... fields) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException;
}
