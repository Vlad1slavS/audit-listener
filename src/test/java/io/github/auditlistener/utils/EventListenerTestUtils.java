package io.github.auditlistener.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventListenerTestUtils {

    public static void createIndexes(ElasticsearchClient elasticsearchClient) {
        try {
            elasticsearchClient.indices().create(builder -> builder
                    .index("audit-methods")
                    .mappings(m -> m
                            .properties("correlationId", p -> p.keyword(k -> k))
                            .properties("timestamp", p -> p.date(d -> d))
                            .properties("eventType", p -> p.keyword(k -> k))
                            .properties("logLevel", p -> p.keyword(k -> k))
                            .properties("methodName", p -> p.text(t -> t))
                            .properties("args", p -> p.text(t -> t))
                            .properties("result", p -> p.text(t -> t))
                            .properties("errorMessage", p -> p.text(t -> t))
                    )
            );

            elasticsearchClient.indices().create(builder -> builder
                    .index("audit-requests")
                    .mappings(m -> m
                            .properties("correlationId", p -> p.keyword(k -> k))
                            .properties("timestamp", p -> p.date(d -> d))
                            .properties("direction", p -> p.keyword(k -> k))
                            .properties("method", p -> p.keyword(k -> k))
                            .properties("uri", p -> p.text(t -> t))
                            .properties("statusCode", p -> p.integer(i -> i))
                            .properties("requestBody", p -> p.text(t -> t))
                            .properties("responseBody", p -> p.text(t -> t))
                    )
            );

            elasticsearchClient.indices().create(builder -> builder
                    .index("audit-errors")
                    .mappings(m -> m
                            .properties("timestamp", p -> p.date(d -> d))
                            .properties("errorType", p -> p.keyword(k -> k))
                            .properties("errorMessage", p -> p.text(t -> t))
                            .properties("originalTopic", p -> p.keyword(k -> k))
                            .properties("originalMessage", p -> p.text(t -> t))
                            .properties("processingStage", p -> p.keyword(k -> k))
                    )
            );

        } catch (Exception ignored) {

        }
    }

    public static void deleteIndexes(ElasticsearchClient elasticsearchClient) {
        try {
            elasticsearchClient.indices().delete(d -> d.index("audit-methods"));
        } catch (Exception ignored) {
        }

        try {
            elasticsearchClient.indices().delete(d -> d.index("audit-requests"));
        } catch (Exception ignored) {
        }

        try {
            elasticsearchClient.indices().delete(d -> d.index("audit-errors"));
        } catch (Exception ignored) {
        }
    }

}
