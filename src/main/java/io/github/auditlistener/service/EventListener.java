package io.github.auditlistener.service;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

public interface EventListener {

    /**
     * Обработка событий методов, полученных из Kafka.
     */
    void handleMethodEvent(@Payload String message,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.RECEIVED_KEY) String key,
                           Acknowledgment acknowledgment);

    /**
     * Обработка событий HTTP-запросов, полученных из Kafka.
     */
    void handleHttpEvent(@Payload String message,
                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                         @Header(KafkaHeaders.RECEIVED_KEY) String key,
                         Acknowledgment acknowledgment);

    /**
     * Обработка сообщений об ошибках, полученных из Kafka.
     */
    void handleErrorEvent(@Payload String message,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key,
                          Acknowledgment acknowledgment);

}
