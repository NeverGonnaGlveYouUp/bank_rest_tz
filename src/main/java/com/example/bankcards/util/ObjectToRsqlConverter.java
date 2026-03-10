package com.example.bankcards.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.StringJoiner;

@Slf4j
public class ObjectToRsqlConverter {
    public static String toRsql(Object object) {
        if (object == null) return "";

        StringJoiner joiner = new StringJoiner(";");

        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(object);
                if (value != null && !value.toString().isEmpty() && !field.getName().equals("maskedNumber")) {
                    String fieldName = field.getName();
                    String formattedValue = formatValue(value);
                    joiner.add(fieldName + "=='" + formattedValue + "'");
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field '{}' while generating RSQL for class {}: {}",
                        field.getName(), object.getClass().getSimpleName(), e.getMessage());
            }
        }
        return joiner.toString();
    }

    private static String formatValue(Object value) {
        if (value instanceof java.time.LocalDate) {
            return value.toString();
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        return value.toString();
    }
}
