package io.github.auditlistener.model.elastic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Документ ошибки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDocument {

    private String id;

    private LocalDateTime timestamp;

    private String errorType;

    private String errorMessage;

    private String originalTopic;

    private String originalMessage;

    private String processingStage;

}
