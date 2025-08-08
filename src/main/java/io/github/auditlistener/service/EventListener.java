package io.github.auditlistener.service;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

public interface EventListener {

    void handleMethodEvent(@Payload String message,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                  Acknowledgment acknowledgment);

    void handleHttpEvent(@Payload String message,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                Acknowledgment acknowledgment);

}
