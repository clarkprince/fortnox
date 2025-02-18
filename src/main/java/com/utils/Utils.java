package com.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final DateTimeFormatter[] DATE_FORMATTERS = { DateTimeFormatter.ISO_DATE_TIME, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"), DateTimeFormatter.ofPattern("yyyy-MM-dd") // Add simple date format
    };

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String prettyPrint(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);
            return jsonNode.toPrettyString();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static LocalDateTime parseDateTime(String dateStr) {
        if (isEmpty(dateStr)) {
            return LocalDateTime.now().minusYears(1);
        }

        log.debug("Attempting to parse date: {}", dateStr);

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (dateStr.length() <= 10) { // If it's just a date without time
                    // Parse as LocalDate and convert to start of day
                    return LocalDateTime.parse(dateStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else {
                    return LocalDateTime.parse(dateStr, formatter);
                }
            } catch (DateTimeParseException e) {
                log.debug("Failed to parse with formatter {}: {}", formatter, e.getMessage());
                continue;
            }
        }

        log.error("Could not parse date '{}' with any known format", dateStr);
        return LocalDateTime.now().minusYears(1);
    }
}
