package com.thoughtworks.fms.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;

public class Json {
    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new DateTimeModule());
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
    }

    public static String toJson(Object obj) {
        String string;
        try {
            string = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return string;
    }

    public static <T> T parseJson(String json, Class<T> klass) {
        try {
            return mapper.readValue(json, klass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DateTimeModule extends SimpleModule {
        public DateTimeModule() {
            addSerializer(DateTime.class, new JsonSerializer<DateTime>() {
                @Override
                public void serialize(DateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeNumber(value.getMillis());
                }
            });

            addDeserializer(DateTime.class, new JsonDeserializer<DateTime>() {

                @Override
                public DateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                    Long mills = jp.readValueAs(Long.class);
                    return new DateTime(mills, DateTimeZone.UTC);
                }
            });
        }
    }

}

