package io.github.auditlistener.service;

public interface KafkaService {

    /**
     * Отправляет сообщение об ошибке в Kafka (error топик)
     *
     * @param errorType тип ошибки
     * @param errorMessage сообщение об ошибке
     * @param originalTopic исходный topic, где произошла ошибка
     * @param originalMessage исходное сообщение
     */
    void sendErrorMessage(String errorType, String errorMessage, String originalTopic,
                          String originalMessage);

}

