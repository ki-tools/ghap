package io.ghap.reporting.service.impl;

import com.google.common.base.Joiner;
import io.ghap.reporting.service.CSVService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class CSVServiceBean implements CSVService {

    @Override
    public void printRecords(Appendable out, List records, List<String> header, String... fields) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL);
        List data = new ArrayList();
        if (header != null && !header.isEmpty()) {
            data.add(header);
        }
        if (records != null && fields != null && fields.length > 0) {
            for (Object obj : records) {
				List row = new ArrayList(fields.length);
				for (String field : fields) {
                    String[] arr = BeanUtils.getArrayProperty(obj, field);
                    String val = (arr != null) ? Joiner.on(",").join(arr):null;
					row.add(val);
				}
				data.add(row);
			}
        }
        printer.printRecords(data);
    }
}
