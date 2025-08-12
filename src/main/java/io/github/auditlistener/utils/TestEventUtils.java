package io.github.auditlistener.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TestEventUtils {

    public static Map<String, Object> createMethodStartEvent(String correlationId) {
        return createMethodEvent(correlationId, "START", "DEBUG", null);
    }

    public static Map<String, Object> createMethodEndEvent(String correlationId) {
        return createMethodEvent(correlationId, "END", "DEBUG", null);
    }

    public static Map<String, Object> createMethodErrorEvent(String correlationId) {
        return createMethodEvent(correlationId, "ERROR", "ERROR", "Test error message");
    }

    private static Map<String, Object> createMethodEvent(String correlationId, String eventType, String logLevel, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("eventType", eventType);
        event.put("methodName", "TestClass.testMethod");
        event.put("logLevel", logLevel);
        event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (errorMessage != null) {
            event.put("errorMessage", errorMessage);
        }
        return event;
    }

    public static Map<String, Object> createHttpIncomingEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("method", "GET");
        event.put("statusCode", 200);
        event.put("uri", "/api/test");
        event.put("direction", "INCOMING");
        event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", 123);
        responseBody.put("name", "Test Response");
        event.put("responseBody", responseBody);

        return event;
    }

    public static Map<String, Object> createHttpOutgoingEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("method", "POST");
        event.put("statusCode", 201);
        event.put("uri", "/api/test2/getUser");
        event.put("direction", "OUTGOING");
        event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "Test user created");
        responseBody.put("id", 456);
        event.put("responseBody", responseBody);

        return event;
    }

}
