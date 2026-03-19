package com.masjidapp.controller.stripe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.HasId;
import com.stripe.model.StripeObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StripeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toSingleLineJson(StripeObject object) {
        if (object == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing Stripe object to JSON for logging", e);
            String id = (object instanceof HasId) ? ((HasId) object).getId() : "N/A";
            return String.format("<%s id=%s>", object.getClass().getSimpleName(), id);
        }
    }

    public static String toSingleLineJson(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return "\"\"";
        }
        try {
            Object jsonObject = objectMapper.readValue(jsonString, Object.class);
            return objectMapper.writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            log.error("Error compacting JSON string for logging", e);
            return jsonString.replaceAll("\\s+", " ").replace("\"", "\\\"");
        }
    }
}
