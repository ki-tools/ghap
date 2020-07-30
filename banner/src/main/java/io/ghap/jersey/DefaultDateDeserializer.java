package io.ghap.jersey;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.node.TextNode;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DefaultDateDeserializer extends JsonDeserializer<Date> {

    private String dateFormat;

    public DefaultDateDeserializer(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec codec = jp.getCodec();
        TextNode node = (TextNode)codec.readTree(jp);
        String dateString = node.getTextValue();
        try {
            return new SimpleDateFormat(dateFormat).parse(dateString);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }
}
