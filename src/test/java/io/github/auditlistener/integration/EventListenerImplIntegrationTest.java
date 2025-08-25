package io.github.auditlistener.integration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.auditlistener.config.KafkaConfig;
import io.github.auditlistener.config.ListenerConfig;
import io.github.auditlistener.model.elastic.ErrorDocument;
import io.github.auditlistener.model.elastic.HttpDocument;
import io.github.auditlistener.model.elastic.MethodDocument;
import io.github.auditlistener.repository.EventRepository;
import io.github.auditlistener.service.impl.ElasticSearchServiceImpl;
import io.github.auditlistener.service.impl.EventListenerImpl;
import io.github.auditlistener.service.impl.KafkaServiceImpl;
import io.github.auditlistener.utils.EventListenerTestUtils;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        EventListenerImplIntegrationTest.TestConfig.class,
        EventListenerImpl.class,
        ElasticSearchServiceImpl.class,
        KafkaServiceImpl.class,
        KafkaConfig.class
})
@Testcontainers
public class EventListenerImplIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("elasticsearch:9.1.2")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventRepository eventRepository;

    private KafkaProducer<String, String> kafkaProducer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("audit.listener.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("audit.listener.kafka.group-id", () -> "test-audit-listener-group");
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @BeforeEach
    void setUp() {

        EventListenerTestUtils.deleteIndexes(elasticsearchClient);

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        kafkaProducer = new KafkaProducer<>(producerProps);

        EventListenerTestUtils.createIndexes(elasticsearchClient);

    }


    @Test
    void processValidMethodData_Success() throws Exception {

        String correlationId = "test-correlation-123";
        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> methodEvent = new HashMap<>();
        methodEvent.put("correlationId", correlationId);
        methodEvent.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        methodEvent.put("eventType", "START");
        methodEvent.put("logLevel", "DEBUG");
        methodEvent.put("methodName", "TestService.testMethod");
        methodEvent.put("arguments", new Object[]{"arg1", "arg2"});
        methodEvent.put("result", "success");

        String message = objectMapper.writeValueAsString(methodEvent);

        kafkaProducer.send(new ProducerRecord<>("audit.methods", correlationId, message));
        kafkaProducer.flush();

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    SearchResponse<MethodDocument> response = elasticsearchClient.search(
                            SearchRequest.of(s -> s
                                    .index("audit-methods")
                                    .query(q -> q.match(m -> m.field("correlationId").query(correlationId)))
                            ),
                            MethodDocument.class
                    );

                    assertFalse(response.hits().hits().isEmpty());
                    Hit<MethodDocument> hit = response.hits().hits().get(0);
                    MethodDocument document = hit.source();

                    assertNotNull(document);
                    assertEquals(correlationId, document.getCorrelationId());
                    assertEquals("START", document.getEventType());
                    assertEquals("DEBUG", document.getLevel());
                    assertEquals("TestService.testMethod", document.getMethod());
                    assertNotNull(document.getArgs());
                    assertEquals("success", document.getResult());
                });
    }

    @Test
    void processValidHttpData_Success() throws Exception {
        String correlationId = "test-correlation-123";
        LocalDateTime timestamp = LocalDateTime.now();

        Map<String, Object> httpEvent = new HashMap<>();
        httpEvent.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        httpEvent.put("direction", "INCOMING");
        httpEvent.put("method", "GET");
        httpEvent.put("uri", "/api/test");
        httpEvent.put("statusCode", 200);
        httpEvent.put("requestBody", "test request");
        httpEvent.put("responseBody", "test response");

        String message = objectMapper.writeValueAsString(httpEvent);

        kafkaProducer.send(new ProducerRecord<>("audit.requests", correlationId, message));
        kafkaProducer.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SearchResponse<HttpDocument> response = elasticsearchClient.search(
                    SearchRequest.of(s -> s
                            .index("audit-requests")
                            .query(q -> q.match(m -> m.field("correlationId").query(correlationId)))
                    ),
                    HttpDocument.class
            );

            assertFalse(response.hits().hits().isEmpty());
            Hit<HttpDocument> hit = response.hits().hits().getFirst();
            HttpDocument document = hit.source();

            assertNotNull(document);
            assertEquals(correlationId, document.getCorrelationId());
            assertEquals("INCOMING", document.getDirection());
            assertEquals("GET", document.getMethod());
            assertEquals("/api/test", document.getUri());
            assertEquals(200, document.getStatusCode());
            assertEquals("test request", document.getRequestBody());
            assertEquals("test response", document.getResponseBody());
        });
    }

    @Test
    void processInvalidMethodData_sendToErrorTopic() throws Exception {
        Map<String, Object> invalidEvent = new HashMap<>();
        invalidEvent.put("correlationId", "invalid-correlation");

        String message = objectMapper.writeValueAsString(invalidEvent);

        kafkaProducer.send(new ProducerRecord<>("audit.methods", "invalid-key", message));
        kafkaProducer.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SearchResponse<ErrorDocument> response = elasticsearchClient.search(
                    SearchRequest.of(s -> s
                            .index("audit-errors")
                            .query(q -> q.match(m -> m.field("errorType").query("VALIDATION_ERROR")))
                    ),
                    ErrorDocument.class
            );

            assertFalse(response.hits().hits().isEmpty());
            Hit<ErrorDocument> hit = response.hits().hits().getFirst();
            ErrorDocument errorDocument = hit.source();

            assertNotNull(errorDocument);
            assertEquals("VALIDATION_ERROR", errorDocument.getErrorType());
            assertEquals("Required fields missing", errorDocument.getErrorMessage());
            assertEquals("audit.methods", errorDocument.getOriginalTopic());
        });
    }


    @Test
    void processInvalidHttpData_sendToErrorTopic() throws Exception {
        Map<String, Object> invalidHttpEvent = new HashMap<>();
        invalidHttpEvent.put("direction", "INCOMING");

        String message = objectMapper.writeValueAsString(invalidHttpEvent);

        kafkaProducer.send(new ProducerRecord<>("audit.requests", "invalid-http-key", message));
        kafkaProducer.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SearchResponse<ErrorDocument> response = elasticsearchClient.search(
                    SearchRequest.of(s -> s
                            .index("audit-errors")
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.match(mt -> mt.field("errorType").query("VALIDATION_ERROR")))
                                    .must(m -> m.match(mt -> mt.field("originalTopic").query("audit.requests")))
                            ))
                    ),
                    ErrorDocument.class
            );

            assertFalse(response.hits().hits().isEmpty());
            Hit<ErrorDocument> hit = response.hits().hits().get(0);
            ErrorDocument errorDocument = hit.source();

            assertNotNull(errorDocument);
            assertEquals("VALIDATION_ERROR", errorDocument.getErrorType());
            assertEquals("audit.requests", errorDocument.getOriginalTopic());
        });
    }

    @Test
    void processErrorData_Success() throws Exception {
        ErrorDocument errorDoc = ErrorDocument.builder()
                .id("error-123")
                .timestamp(LocalDateTime.now())
                .errorType("TEST_ERROR")
                .errorMessage("Test error message")
                .originalTopic("audit.methods")
                .originalMessage("original message")
                .processingStage("VALIDATION")
                .build();

        String message = objectMapper.writeValueAsString(errorDoc);

        kafkaProducer.send(new ProducerRecord<>("audit.errors", "error-key", message));
        kafkaProducer.flush();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SearchResponse<ErrorDocument> response = elasticsearchClient.search(
                    SearchRequest.of(s -> s
                            .index("audit-errors")
                            .query(q -> q.match(m -> m.field("errorType").query("TEST_ERROR")))
                    ),
                    ErrorDocument.class
            );

            assertFalse(response.hits().hits().isEmpty());
            Hit<ErrorDocument> hit = response.hits().hits().get(0);
            ErrorDocument document = hit.source();

            assertNotNull(document);
            assertEquals("TEST_ERROR", document.getErrorType());
            assertEquals("Test error message", document.getErrorMessage());
            assertEquals("audit.methods", document.getOriginalTopic());
            assertEquals("VALIDATION", document.getProcessingStage());
        });
    }

    @TestConfiguration
    @EnableKafka
    static class TestConfig {

        @Bean
        @Primary
        public ObjectMapper testObjectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        @Bean
        @Primary
        public RestClient testRestClient() {
            return RestClient.builder(
                    HttpHost.create("http://" + elasticsearch.getHttpHostAddress())
            ).build();
        }

        @Bean
        @Primary
        public ElasticsearchClient testElasticsearchClient(RestClient restClient, ObjectMapper objectMapper) {
            return new ElasticsearchClient(
                    new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper))
            );
        }

        @Bean
        public ProducerFactory<String, String> testProducerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public KafkaTemplate<String, String> testKafkaTemplate(ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        @Primary
        public ListenerConfig listenerConfig() {
            ListenerConfig config = new ListenerConfig();
            config.getKafka().setBootstrapServers(kafka.getBootstrapServers());
            config.getKafka().setGroupId("test-audit-listener-group");
            config.getKafka().setMethodTopic("audit.methods");
            config.getKafka().setHttpTopic("audit.requests");
            config.getKafka().setErrorTopic("audit.errors");
            return config;
        }
    }
}