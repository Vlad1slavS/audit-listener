package io.github.auditlistener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.auditlistener.config.ListenerConfig;
import io.github.auditlistener.model.elastic.ErrorDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для работы с Kafka
 */
@Service
public class KafkaServiceImpl implements KafkaService {

    private final Logger log = LogManager.getLogger(KafkaServiceImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ListenerConfig config;
    private final ObjectMapper objectMapper;

    public KafkaServiceImpl(KafkaTemplate<String, String> kafkaTemplate,
                            ListenerConfig config,
                            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public void sendErrorMessage(String errorType, String errorMessage, String originalTopic,
                                 String originalMessage) {
        try {
            ErrorDocument errorDoc = ErrorDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .errorType(errorType)
                    .errorMessage(errorMessage)
                    .originalTopic(originalTopic)
                    .originalMessage(originalMessage)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(errorDoc);
            String key = "error_" + errorDoc.getId();

            kafkaTemplate.send(config.getKafka().getErrorTopic(), key, jsonMessage);
            log.debug("Error sent to Kafka: {}", errorType);

        } catch (Exception e) {
            log.error("Failed to send error to Kafka: {}", errorMessage, e);
        }
    }

}
