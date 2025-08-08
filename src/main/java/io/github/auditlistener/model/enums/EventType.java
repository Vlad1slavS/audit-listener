package io.github.auditlistener.model.enums;

public enum EventType {

    START,
    END,
    ERROR;

    public static EventType getByName(String eventType) {
        if (eventType == null) {
            return null;
        }
        try {
            return EventType.valueOf(eventType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown event type: " + eventType, e);
        }
    }

}
