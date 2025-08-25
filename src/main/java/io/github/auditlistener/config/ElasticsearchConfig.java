package io.github.auditlistener.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ElasticSearch конфигурация (только для добавления ObjectMapper для работы с датами)
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new ElasticsearchClient(
                new RestClientTransport(restClient, new JacksonJsonpMapper(mapper))
        );
    }

}
