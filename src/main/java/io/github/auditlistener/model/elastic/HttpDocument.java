package io.github.auditlistener.model.elastic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

/**
 * Документ запроса
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "audit-requests")
public class HttpDocument {

    private String id;

    private String correlationId;

    private LocalDateTime timestamp;

    private String direction;

    private String method;

    private String uri;

    private Integer statusCode;

    private String requestBody;

    private String responseBody;

}
