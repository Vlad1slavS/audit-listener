package io.github.auditlistener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.auditlistener.model.elastic.ErrorDocument;
import io.github.auditlistener.model.elastic.HttpDocument;
import io.github.auditlistener.model.elastic.MethodDocument;
import io.github.auditlistener.utils.EventValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Сервис "вычитки" сообщений из Kafka
 */
@Service
public class EventListenerImpl implements EventListener {

    private final Logger log = LogManager.getLogger(EventListenerImpl.class);

//    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaServiceImpl kafkaService;
    private final ElasticSearchServiceImpl elasticsearchService;

    public EventListenerImpl(ObjectMapper objectMapper,
                             KafkaServiceImpl errorKafkaService, ElasticSearchServiceImpl elasticsearchService) {
//        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.kafkaService = errorKafkaService;
        this.elasticsearchService = elasticsearchService;
    }

    /**
     * Обработка событий метода
     */
    @KafkaListener(topics = "audit.methods", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleMethodEvent(@Payload String message,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                  Acknowledgment acknowledgment) {

        log.debug("Received method audit message from topic: {}, key: {}",
                topic, key);

        try {
            JsonNode event;
            try {
                event = objectMapper.readTree(message);
            } catch (Exception e) {
                kafkaService.sendErrorMessage("PARSING_ERROR", e.getMessage(), topic, message);
                acknowledgment.acknowledge();
                return;
            }

            if (!EventValidator.validateMethodEvent(event)) {
                kafkaService.sendErrorMessage("VALIDATION_ERROR", "Required fields missing", topic, message);
                acknowledgment.acknowledge();
                return;
            }

            MethodDocument document = MethodDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .correlationId(event.get("correlationId").asText())
                    .timestamp(LocalDateTime.parse(event.get("timestamp").asText()))
                    .eventType(event.get("eventType").asText())
                    .logLevel(event.get("logLevel").asText())
                    .methodName(event.get("methodName").asText())
                    .args(event.hasNonNull("arguments")
                            ? Arrays.toString(objectMapper.convertValue(event.get("arguments"), Object[].class))
                            : null)
                    .result(event.hasNonNull("result")
                            ? event.get("result").asText()
                            : null)
                    .errorMessage(event.hasNonNull("errorMessage")
                            ? event.get("errorMessage").asText()
                            : null)
                    .build();

            try {
                elasticsearchService.indexMethodDocument(document);
                log.debug("Successfully indexed method event with correlation ID: {}", document.getCorrelationId());
            } catch (Exception e) {
                kafkaService.sendErrorMessage("INDEXING_ERROR", e.getMessage(), topic, message);
                acknowledgment.acknowledge();
                return;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Unexpected error processing method audit message: {}", message, e);
            kafkaService.sendErrorMessage("PROCESSING_ERROR", e.getMessage(), topic, message);
            acknowledgment.acknowledge();
        }

    }

    /**
     * Обработка HTTP событий
     */
    @KafkaListener(topics = "audit.requests", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleHttpEvent(@Payload String message,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                Acknowledgment acknowledgment) {

        log.debug("Received HTTP audit message from topic: {}, key: {}",
                topic, key);

        try {
            JsonNode event;
            try {
                event = objectMapper.readTree(message);
            } catch (Exception e) {
                kafkaService.sendErrorMessage("PARSING_ERROR", e.getMessage(), topic, message);
                acknowledgment.acknowledge();
                return;
            }

            if (!EventValidator.validateHttpEvent(event)) {
                kafkaService.sendErrorMessage("VALIDATION_ERROR", "Required fields missing", topic, message);
                acknowledgment.acknowledge();
                return;
            }

            HttpDocument document = HttpDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .correlationId(key)
                    .timestamp(LocalDateTime.parse(event.get("timestamp").asText()))
                    .direction(event.get("direction").asText())
                    .method(event.get("method").asText())
                    .uri(event.get("uri").asText())
                    .statusCode(event.get("statusCode").asInt())
                    .requestBody(event.hasNonNull("requestBody")
                            ? event.get("requestBody").asText()
                            : null)
                    .responseBody(event.hasNonNull("responseBody")
                            ? event.get("responseBody").asText()
                            : null)
                    .build();

            try {
                elasticsearchService.indexHttpDocument(document);
                log.debug("Successfully indexed HTTP event for URI: {}", document.getUri());
            } catch (Exception e) {
                kafkaService.sendErrorMessage("INDEXING_ERROR", e.getMessage(), topic, message);
                acknowledgment.acknowledge();
                return;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Unexpected error processing HTTP audit message: {}", message, e);
            kafkaService.sendErrorMessage("PROCESSING_ERROR", e.getMessage(), topic, message);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = "audit.errors", containerFactory = "kafkaListenerContainerFactory")
    public void handleErrorEvent(@Payload String message,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 Acknowledgment acknowledgment) {

        log.debug("Received error message from topic: {}, key: {}", topic, key);

        try {
            JsonNode errorNode = objectMapper.readTree(message);
            ErrorDocument errorDocument =
                    objectMapper.convertValue(errorNode, ErrorDocument.class);

            elasticsearchService.indexErrorDocument(errorDocument);
            log.debug("Successfully indexed error event");
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process error message: {}", message, e);
            acknowledgment.acknowledge();
        }
    }

}
