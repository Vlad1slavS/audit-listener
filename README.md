# audit-listener

Микросервис для обработки и сохранения событий аудита из Kafka в PostgreSQL.

Audit-listener предназначен для получения событий аудита из двух Kafka топиков:
- **contractor-audit-method-topic** - события выполнения методов
- **contractor-audit-http-topic** - события HTTP запросов

## Технологический стек

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Kafka** - для интеграции с Apache Kafka
- **Spring Data JPA** - для работы с базой данных
- **PostgreSQL** - основная база данных
- **Jackson** - для работы с JSON
- **Lombok** - для упрощения кода
- **TestContainers** - для интеграционных тестов

## Типы событий

### Method Events (METHOD источник)

Формат сообщения для методов:
```json
{
  "correlationId": "uuid",
  "eventType": "START | END | ERROR",
  "methodName": "ClassName.methodName",
  "logLevel": "DEBUG | INFO | ERROR | TRACE | FATAL",
  "timestamp": "2024-01-01T12:00:00",
  "errorMessage": "Error details (для ERROR событий)",
  "arguments": ["arg1", "arg2"]
  
}
```

### HTTP Events (HTTP источник)

Формат сообщения для HTTP:
```json
{
  "method": "GET | POST | PUT | DELETE",
  "statusCode": 200,
  "uri": "/api/endpoint",
  "direction": "Incoming | Outgoing",
  "timestamp": "2024-01-01T12:00:00",
  "responseBody": {},
}
```