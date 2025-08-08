package io.github.auditlistener.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.auditlistener.model.enums.EventSource;
import io.github.auditlistener.model.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Событие
 */
@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id")
    @NotNull
    @NotBlank
    private String correlationId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "log_level")
    private String logLevel;

    @Column(name = "event_source")
    @Enumerated(EnumType.STRING)
    private EventSource eventSource;

    @Column(name = "target_name")
    private String targetName;

    @Column(name = "data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode data;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "uri")
    private String uri;

    @Column(name = "direction")
    private String direction;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

}
