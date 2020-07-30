package io.ghap.util;

import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.Date;

public class Iso8601 {

    public static String get(Date date) {
        DateTime dt = new DateTime(date.getTime());
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dt);
    }

    public static Date get(String str) {
        DateTime dt = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return new Date(fmt.parseDateTime(str).getMillis());
    }

    public static JsonSerializer<Date> getJsonSerializer(){
        return new JsonSerializer<Date>();
    }

    public static JsonDeserializer<Date> getJsonDeserializer(){
        return new JsonDeserializer<Date>();
    }

    private static class JsonDeserializer<Date> implements com.google.gson.JsonDeserializer<java.util.Date> {
        @Override
        public java.util.Date deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context) throws JsonParseException {
            //return json == null ? null : new Date(json.getAsLong());
            return json == null ? null : Iso8601.get(json.getAsString());
        }
    }

    private static class JsonSerializer<Date> implements com.google.gson.JsonSerializer<java.util.Date> {
        @Override
        public JsonElement serialize(java.util.Date src, Type typeOfSrc, JsonSerializationContext
                context) {
            //return src == null ? null : new JsonPrimitive(src.getTime());
            return src == null ? null : new JsonPrimitive(Iso8601.get(src));
        }
    }
}
