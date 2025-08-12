package io.github.auditlistener;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.auditlistener.model.entity.Event;
import io.github.auditlistener.model.enums.EventSource;
import io.github.auditlistener.model.enums.EventType;
import io.github.auditlistener.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.github.auditlistener.utils.TestEventUtils.createHttpIncomingEvent;
import static io.github.auditlistener.utils.TestEventUtils.createHttpOutgoingEvent;
import static io.github.auditlistener.utils.TestEventUtils.createMethodEndEvent;
import static io.github.auditlistener.utils.TestEventUtils.createMethodErrorEvent;
import static io.github.auditlistener.utils.TestEventUtils.createMethodStartEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        ports = {9094},
        topics = {"contractor-audit-method-topic", "contractor-audit-http-topic"},
        brokerProperties = {
                "enable.idempotence=true",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.consumer.group-id=test-audit-listener-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "audit.kafka.method-topic=contractor-audit-method-topic",
        "audit.kafka.http-topic=contractor-audit-http-topic"
})
public class ListenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("test_audit_db")
            .withUsername("test_user")
            .withPassword("12345");

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    void getMethodStartEvent_StartMethodEvent() throws Exception {

        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> methodEvent = createMethodStartEvent(correlationId);
        String jsonMessage = objectMapper.writeValueAsString(methodEvent);

        kafkaTemplate.send("contractor-audit-method-topic", correlationId, jsonMessage);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> eventRepository.findByCorrelationIdOrderByTimestamp(correlationId).size() == 1);

        List<Event> events = eventRepository.findByCorrelationIdOrderByTimestamp(correlationId);
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getEventType()).isEqualTo(EventType.START);
        assertThat(event.getEventSource()).isEqualTo(EventSource.METHOD);
        assertThat(event.getTargetName()).isEqualTo("TestClass.testMethod");
        assertThat(event.getLogLevel()).isEqualTo("DEBUG");

    }

    @Test
    void getMethodEndEvent_MethodEndEvent() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> methodEvent = createMethodEndEvent(correlationId);
        String jsonMessage = objectMapper.writeValueAsString(methodEvent);

        kafkaTemplate.send("contractor-audit-method-topic", correlationId, jsonMessage);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> eventRepository.findByCorrelationIdOrderByTimestamp(correlationId).size() == 1);

        List<Event> events = eventRepository.findByCorrelationIdOrderByTimestamp(correlationId);
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getEventType()).isEqualTo(EventType.END);
        assertThat(event.getEventSource()).isEqualTo(EventSource.METHOD);
        assertThat(event.getTargetName()).isEqualTo("TestClass.testMethod");
        assertThat(event.getLogLevel()).isEqualTo("DEBUG");

    }


    @Test
    void shouldProcessMethodErrorEvent() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> methodEvent = createMethodErrorEvent(correlationId);
        String jsonMessage = objectMapper.writeValueAsString(methodEvent);

        kafkaTemplate.send("contractor-audit-method-topic", correlationId, jsonMessage);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> eventRepository.findByCorrelationIdOrderByTimestamp(correlationId).size() == 1);

        List<Event> events = eventRepository.findByCorrelationIdOrderByTimestamp(correlationId);
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getEventType()).isEqualTo(EventType.ERROR);
        assertThat(event.getEventSource()).isEqualTo(EventSource.METHOD);
        assertThat(event.getTargetName()).isEqualTo("TestClass.testMethod");
        assertThat(event.getLogLevel()).isEqualTo("ERROR");
        assertThat(event.getErrorMessage()).isEqualTo("Test error message");

    }

    @Test
    void shouldProcessHttpIncomingEvent() throws Exception {
        String key = "INCOMING_GET_" + System.currentTimeMillis();
        Map<String, Object> httpEvent = createHttpIncomingEvent();
        String jsonMessage = objectMapper.writeValueAsString(httpEvent);

        kafkaTemplate.send("contractor-audit-http-topic", key, jsonMessage);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> eventRepository.findByCorrelationIdOrderByTimestamp(key).size() == 1);

        List<Event> events = eventRepository.findByCorrelationIdOrderByTimestamp(key);
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getCorrelationId()).isEqualTo(key);
        assertThat(event.getEventSource()).isEqualTo(EventSource.HTTP);
        assertThat(event.getHttpMethod()).isEqualTo("GET");
        assertThat(event.getHttpStatus()).isEqualTo(200);
        assertThat(event.getUri()).isEqualTo("/api/test");
        assertThat(event.getDirection()).isEqualTo("INCOMING");
        assertThat(event.getLogLevel()).isEqualTo("INFO");

    }

    @Test
    void shouldProcessHttpOutgoingEvent() throws Exception {
        String key = "OUTGOING_POST_" + System.currentTimeMillis();
        Map<String, Object> httpEvent = createHttpOutgoingEvent();
        String jsonMessage = objectMapper.writeValueAsString(httpEvent);

        kafkaTemplate.send("contractor-audit-http-topic", key, jsonMessage);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> eventRepository.findByCorrelationIdOrderByTimestamp(key).size() == 1);

        List<Event> events = eventRepository.findByCorrelationIdOrderByTimestamp(key);
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getCorrelationId()).isEqualTo(key);
        assertThat(event.getEventSource()).isEqualTo(EventSource.HTTP);
        assertThat(event.getHttpMethod()).isEqualTo("POST");
        assertThat(event.getHttpStatus()).isEqualTo(201);
        assertThat(event.getUri()).isEqualTo("/api/test2/getUser");
        assertThat(event.getDirection()).isEqualTo("OUTGOING");

    }

}
