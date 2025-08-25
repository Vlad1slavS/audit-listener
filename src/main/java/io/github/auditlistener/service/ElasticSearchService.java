package io.github.auditlistener.service;

import io.github.auditlistener.model.elastic.ErrorDocument;
import io.github.auditlistener.model.elastic.HttpDocument;
import io.github.auditlistener.model.elastic.MethodDocument;

public interface ElasticSearchService {

    /**
     * Индексирует документ метода
     *
     * @param document документ метода для индексации
     * @throws RuntimeException Ошибка индексации
     */
    void indexMethodDocument(MethodDocument document);

    /**
     * Индексирует HTTP документ
     *
     * @param document HTTP документ для индексации
     * @throws RuntimeException Ошибка индексации
     */
    void indexHttpDocument(HttpDocument document);

    /**
     * Индексирует документ ошибки
     *
     * @param document документ ошибки для индексации
     */
    void indexErrorDocument(ErrorDocument document);

}

