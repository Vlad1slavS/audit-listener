package io.github.auditlistener.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventValidator {

    public static boolean validateMethodEvent(JsonNode eventNode) {
        return eventNode.has("correlationId") && !eventNode.get("correlationId").isNull() &&
                eventNode.has("timestamp") && !eventNode.get("timestamp").isNull() &&
                eventNode.has("eventType") && !eventNode.get("eventType").isNull() &&
                eventNode.has("logLevel") && !eventNode.get("logLevel").isNull() &&
                eventNode.has("methodName") && !eventNode.get("methodName").isNull();
    }

    public static boolean validateHttpEvent(JsonNode eventNode) {
        return eventNode.has("timestamp") && !eventNode.get("timestamp").isNull() &&
                eventNode.has("direction") && !eventNode.get("direction").isNull() &&
                eventNode.has("method") && !eventNode.get("method").isNull() &&
                eventNode.has("uri") && !eventNode.get("uri").isNull() &&
                eventNode.has("statusCode") && !eventNode.get("statusCode").isNull();
    }

}
