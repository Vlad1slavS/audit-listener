package io.github.auditlistener.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурационные свойства для Kafka
 */
@ConfigurationProperties(prefix = "audit.listener")
@Data
public class ListenerConfig {

    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {
        private String bootstrapServers = "localhost:9092";
        private String groupId = "audit-listener-group";
        private String methodTopic = "audit.methods";
        private String httpTopic = "audit.requests";
        private String errorTopic = "audit.errors";
        private String autoOffsetReset = "earliest";
        private int maxPollRecords = 1;
        private String isolationLevel = "read_committed";
        private boolean enableAutoCommit = false;
        private int sessionTimeoutMs = 30000;
        private int heartbeatIntervalMs = 10000;
        private int maxPollIntervalMs = 300000;
    }

}
