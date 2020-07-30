package io.ghap.jersey;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DefaultDateSerializer extends JsonSerializer<Date> {

    private String dateFormat;

    public DefaultDateSerializer(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        if (value == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(new SimpleDateFormat(dateFormat).format(value));
        }
    }
}
