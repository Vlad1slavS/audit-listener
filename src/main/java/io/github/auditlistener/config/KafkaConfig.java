package io.github.auditlistener.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация kafka consumer
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers:localhost:9094}")
    private String bootstrapServers;

    @Value("${kafka.group.id:audit-listener-group}")
    private String groupId;

    @Value("${kafka.auto.offset.reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.enable.auto.commit:false}")
    private boolean enableAutoCommit;

    @Value("${kafka.isolation.level:read_committed}")
    private String isolationLevel;

    @Value("${kafka.max.poll.records:1}")
    private int maxPollRecords;

    @Value("${kafka.session.timeout.ms:30000}")
    private int sessionTimeoutMs;

    @Value("${kafka.heartbeat.interval.ms:3000}")
    private int heartbeatIntervalMs;

    @Value("${kafka.max.poll.interval.ms:300000}")
    private int maxPollIntervalMs;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        return new DefaultKafkaConsumerFactory<>(configProps);

    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);

        return factory;
    }

}
