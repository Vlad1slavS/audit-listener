package io.github.auditlistener.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import io.github.auditlistener.model.elastic.ErrorDocument;
import io.github.auditlistener.model.elastic.HttpDocument;
import io.github.auditlistener.model.elastic.MethodDocument;
import io.github.auditlistener.service.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с ElasticSearch (индексации документов)
 */
@Service
@RequiredArgsConstructor
public class ElasticSearchServiceImpl implements ElasticSearchService {

    private final ElasticsearchClient elasticsearchClient;

    private static final String METHOD_INDEX = "audit-methods";
    private static final String HTTP_INDEX = "audit-requests";
    private static final String ERROR_INDEX = "audit-errors";

    private final Logger log = LogManager.getLogger(ElasticSearchServiceImpl.class);

    public void indexMethodDocument(MethodDocument document) {
        try {

            IndexRequest<MethodDocument> request = IndexRequest.of(i -> i
                    .index(METHOD_INDEX)
                    .id(document.getId())
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("Method document indexed successfully: {} with result: {}",
                    document.getId(), response.result());

        } catch (Exception e) {
            log.error("Failed to index method document: {}", document, e);
            throw new RuntimeException("Failed to index method document", e);
        }
    }

    public void indexHttpDocument(HttpDocument document) {
        try {
            IndexRequest<HttpDocument> request = IndexRequest.of(i -> i
                    .index(HTTP_INDEX)
                    .id(document.getId())
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("HTTP document indexed successfully: {} with result: {}",
                    document.getId(), response.result());

        } catch (Exception e) {
            log.error("Failed to index HTTP document: {}", document, e);
            throw new RuntimeException("Failed to index HTTP document", e);
        }
    }

    public void indexErrorDocument(ErrorDocument document) {
        try {

            IndexRequest<ErrorDocument> request = IndexRequest.of(i -> i
                    .index(ERROR_INDEX)
                    .id(document.getId())
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("Error document indexed successfully: {} with result: {}",
                    document.getId(), response.result());

        } catch (Exception e) {
            log.error("Failed to index error document: {}", document, e);
        }
    }

}
