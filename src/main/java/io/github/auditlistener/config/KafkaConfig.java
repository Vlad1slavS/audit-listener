package io.github.auditlistener.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(ListenerConfig.class)
public class KafkaConfig {

    private final ListenerConfig config;

    public KafkaConfig(ListenerConfig config) {
        this.config = config;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafka().getBootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, config.getKafka().getGroupId());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getKafka().getAutoOffsetReset());
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.getKafka().isEnableAutoCommit());
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, config.getKafka().getIsolationLevel());
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.getKafka().getMaxPollRecords());
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.getKafka().getSessionTimeoutMs());
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, config.getKafka().getHeartbeatIntervalMs());
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, config.getKafka().getMaxPollIntervalMs());

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
