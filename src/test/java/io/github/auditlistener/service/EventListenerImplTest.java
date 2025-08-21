package io.github.auditlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.auditlistener.model.elastic.ErrorDocument;
import io.github.auditlistener.model.elastic.HttpDocument;
import io.github.auditlistener.model.elastic.MethodDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventListenerImplTest {

    @Mock
    private KafkaServiceImpl kafkaService;

    @Mock
    private ElasticSearchServiceImpl elasticsearchService;

    @Mock
    private Acknowledgment acknowledgment;

    private EventListenerImpl eventListener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        eventListener = new EventListenerImpl(
                objectMapper,
                kafkaService,
                elasticsearchService
        );
    }

    @Test
    void processValidMethodData_Success() throws JsonProcessingException {

        String correlationId = "test-correlation-id";
        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> validMessage = new HashMap<>();
        validMessage.put("correlationId", correlationId);
        validMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        validMessage.put("eventType", "START");
        validMessage.put("logLevel", "DEBUG");
        validMessage.put("methodName", "TestService.testMethod");
        validMessage.put("arguments", new Object[]{"arg1", "arg2"});

        String message = objectMapper.writeValueAsString(validMessage);

        eventListener.handleMethodEvent(message, "audit.methods", "test-key", acknowledgment);

        ArgumentCaptor<MethodDocument> documentCaptor = ArgumentCaptor.forClass(MethodDocument.class);
        verify(elasticsearchService).indexMethodDocument(documentCaptor.capture());
        verify(acknowledgment).acknowledge();
        verify(kafkaService, never()).sendErrorMessage(anyString(), anyString(), anyString(), anyString());

        MethodDocument captured = documentCaptor.getValue();
        assertEquals("test-correlation-id", captured.getCorrelationId());
        assertEquals("START", captured.getEventType());
        assertEquals("DEBUG", captured.getLogLevel());
        assertEquals("TestService.testMethod", captured.getMethodName());
        assertNotNull(captured.getArgs());
    }

    @Test
    void processValidHttpData_Success() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, Object> validMessage = new HashMap<>();
        validMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        validMessage.put("direction", "INCOMING");
        validMessage.put("method", "GET");
        validMessage.put("uri", "/api/test");
        validMessage.put("statusCode", 200);
        validMessage.put("requestBody", "test request");
        validMessage.put("responseBody", "test response");

        String message = objectMapper.writeValueAsString(validMessage);

        eventListener.handleHttpEvent(message, "audit.requests", "correlation-123", acknowledgment);

        ArgumentCaptor<HttpDocument> documentCaptor = ArgumentCaptor.forClass(HttpDocument.class);
        verify(elasticsearchService).indexHttpDocument(documentCaptor.capture());
        verify(acknowledgment).acknowledge();
        verify(kafkaService, never()).sendErrorMessage(anyString(), anyString(), anyString(), anyString());

        HttpDocument captured = documentCaptor.getValue();
        assertEquals("correlation-123", captured.getCorrelationId());
        assertEquals("INCOMING", captured.getDirection());
        assertEquals("GET", captured.getMethod());
        assertEquals("/api/test", captured.getUri());
        assertEquals(200, captured.getStatusCode());
        assertEquals("test request", captured.getRequestBody());
        assertEquals("test response", captured.getResponseBody());
    }

    @Test
    void processInvalidJsonMethodData_SendErrorMessage() {

        String invalidMessage = "invalid json";

        eventListener.handleMethodEvent(invalidMessage, "audit.methods", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("PARSING_ERROR"),
                anyString(),
                eq("audit.methods"),
                eq(invalidMessage)
        );
        verify(elasticsearchService, never()).indexMethodDocument(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processMethodDataWithoutNeededFields_SendErrorMessage() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> incompleteMessage = new HashMap<>();
        incompleteMessage.put("correlationId", "test-correlation-id");
        incompleteMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String message = objectMapper.writeValueAsString(incompleteMessage);

        eventListener.handleMethodEvent(message, "audit.methods", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("VALIDATION_ERROR"),
                eq("Required fields missing"),
                eq("audit.methods"),
                eq(message)
        );
        verify(elasticsearchService, never()).indexMethodDocument(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processMethodData_ElasticsearchError_SendErrorMessage() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> validMessage = new HashMap<>();
        validMessage.put("correlationId", "test-correlation-id");
        validMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        validMessage.put("eventType", "START");
        validMessage.put("logLevel", "DEBUG");
        validMessage.put("methodName", "TestService.testMethod");

        String message = objectMapper.writeValueAsString(validMessage);

        doThrow(new RuntimeException("ES connection error"))
                .when(elasticsearchService).indexMethodDocument(any());

        eventListener.handleMethodEvent(message, "audit.methods", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("INDEXING_ERROR"),
                eq("ES connection error"),
                eq("audit.methods"),
                eq(message)
        );
        verify(acknowledgment).acknowledge();
    }


    @Test
    void processInvalidJsonHttpData_SendErrorMessage() {
        String invalidMessage = "invalid json";

        eventListener.handleHttpEvent(invalidMessage, "audit.requests", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("PARSING_ERROR"),
                anyString(),
                eq("audit.requests"),
                eq(invalidMessage)
        );
        verify(elasticsearchService, never()).indexHttpDocument(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processHttpDataWithoutNeededFields_SendErrorMessage() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, Object> incompleteMessage = new HashMap<>();
        incompleteMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        incompleteMessage.put("direction", "INCOMING");

        String message = objectMapper.writeValueAsString(incompleteMessage);


        eventListener.handleHttpEvent(message, "audit.requests", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("VALIDATION_ERROR"),
                eq("Required fields missing"),
                eq("audit.requests"),
                eq(message)
        );
        verify(elasticsearchService, never()).indexHttpDocument(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processHttpData_ElasticsearchError_SendErrorMessage() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> validMessage = new HashMap<>();
        validMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        validMessage.put("direction", "INCOMING");
        validMessage.put("method", "GET");
        validMessage.put("uri", "/api/test");
        validMessage.put("statusCode", 200);

        String message = objectMapper.writeValueAsString(validMessage);

        doThrow(new RuntimeException("ES indexing failed"))
                .when(elasticsearchService).indexHttpDocument(any());

        eventListener.handleHttpEvent(message, "audit.requests", "test-key", acknowledgment);

        verify(kafkaService).sendErrorMessage(
                eq("INDEXING_ERROR"),
                eq("ES indexing failed"),
                eq("audit.requests"),
                eq(message)
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processValidErrorEvent_Success() throws JsonProcessingException {

        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("id", "error-123");
        errorMessage.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorMessage.put("errorType", "PARSING_ERROR");
        errorMessage.put("errorMessage", "Invalid JSON format");
        errorMessage.put("originalTopic", "audit.methods");
        errorMessage.put("originalMessage", "invalid json");

        String message = objectMapper.writeValueAsString(errorMessage);

        eventListener.handleErrorEvent(message, "audit.errors", "error-key", acknowledgment);

        ArgumentCaptor<ErrorDocument> documentCaptor = ArgumentCaptor.forClass(ErrorDocument.class);
        verify(elasticsearchService).indexErrorDocument(documentCaptor.capture());
        verify(acknowledgment).acknowledge();

        ErrorDocument captured = documentCaptor.getValue();
        assertEquals("error-123", captured.getId());
        assertEquals("PARSING_ERROR", captured.getErrorType());
        assertEquals("Invalid JSON format", captured.getErrorMessage());
        assertEquals("audit.methods", captured.getOriginalTopic());
        assertEquals("invalid json", captured.getOriginalMessage());
    }
    
}