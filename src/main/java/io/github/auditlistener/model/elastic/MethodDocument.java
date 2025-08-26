package io.github.auditlistener.model.elastic;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

/**
 * Документ метода
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "audit-methods")
public class MethodDocument {

    private String id;

    private String correlationId;

    private LocalDateTime timestamp;

    private String eventType;

    private String level;

    private String method;

    private String args;

    private String result;

    private String errorMessage;

}
