package io.github.auditlistener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.auditlistener.model.entity.Event;
import io.github.auditlistener.model.enums.EventSource;
import io.github.auditlistener.model.enums.EventType;
import io.github.auditlistener.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Сервис "вычитки" сообщений из Kafka
 */
@Service
@Slf4j
public class EventListenerImpl implements EventListener {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventListenerImpl(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Обработка событий метода
     */
    @KafkaListener(topics = "contractor-audit-method-topic", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleMethodEvent(@Payload String message,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                  Acknowledgment acknowledgment) {

        log.debug("Received method audit message from topic: {}, key: {}",
                topic, key);

        try {

            JsonNode eventNode = objectMapper.readTree(message);

            Event event = Event.builder()
                    .correlationId(eventNode.hasNonNull("correlationId") ? eventNode.get("correlationId").asText() : null)
                    .eventType(eventNode.hasNonNull("eventType") ? EventType.getByName(eventNode.get("eventType").asText()) : null)
                    .eventSource(EventSource.METHOD)
                    .targetName(eventNode.hasNonNull("methodName") ? eventNode.get("methodName").asText() : null)
                    .logLevel(eventNode.hasNonNull("logLevel") ? eventNode.get("logLevel").asText() : null)
                    .timestamp(eventNode.hasNonNull("timestamp")
                            ? LocalDateTime.parse(eventNode.get("timestamp").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null)
                    .data(eventNode)
                    .errorMessage(eventNode.hasNonNull("errorMessage") ? eventNode.get("errorMessage").asText() : null)
                    .build();

            eventRepository.save(event);

            log.debug("Successfully saved method audit event with correlation ID: {}",
                    event.getCorrelationId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process method audit message: {}", message, e);
            throw new RuntimeException("Failed to process audit message", e);
        }
    }

    /**
     * Обработка HTTP событий
     */
    @KafkaListener(topics = "contractor-audit-http-topic",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleHttpEvent(@Payload String message,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                Acknowledgment acknowledgment) {

        log.debug("Received HTTP audit message from topic: {}, key: {}",
                topic, key);

        try {
            JsonNode eventNode = objectMapper.readTree(message);

            Event auditEvent = Event.builder()
                    .correlationId(key)
                    .eventSource(EventSource.HTTP)
                    .logLevel("INFO")
                    .timestamp(eventNode.hasNonNull("timestamp")
                            ? LocalDateTime.parse(eventNode.get("timestamp").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null)
                    .data(eventNode.hasNonNull("responseBody") ? eventNode.get("responseBody") : null)
                    .httpMethod(eventNode.hasNonNull("method") ? eventNode.get("method").asText() : null)
                    .httpStatus(eventNode.hasNonNull("statusCode") ? eventNode.get("statusCode").asInt() : 0)
                    .uri(eventNode.hasNonNull("uri") ? eventNode.get("uri").asText() : null)
                    .direction(eventNode.hasNonNull("direction") ? eventNode.get("direction").asText() : null)
                    .build();

            eventRepository.save(auditEvent);

            log.debug("Successfully saved HTTP audit event for URI: {}", auditEvent.getUri());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process HTTP audit message: {}", message, e);
            throw new RuntimeException("Failed to process audit message", e);
        }
    }

}
