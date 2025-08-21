#!/bin/bash

ES_HOST=localhost:9200

curl -X PUT "$ES_HOST/audit-methods" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "correlationId": {
        "type": "keyword"
      },
      "timestamp": {
        "type": "date"
      },
      "eventType": {
        "type": "keyword"
      },
      "level": {
        "type": "keyword"
      },
      "method": {
        "type": "text",
        "analyzer": "audit_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "wildcard": {
            "type": "wildcard"
          }
        }
      },
      "args": {
        "type": "text",
        "analyzer": "audit_analyzer"
      },
      "result": {
        "type": "text",
        "analyzer": "audit_analyzer"
      },
      "errorMessage": {
        "type": "text",
        "analyzer": "audit_analyzer"
      }
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "audit_analyzer": {
          "type": "standard",
          "stopwords": "_english_"
        }
      }
    }
  }
}'

echo -e "\n"

curl -X PUT "$ES_HOST/audit-requests" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "correlationId": {
        "type": "keyword"
      },
      "timestamp": {
        "type": "date"
      },
      "direction": {
        "type": "keyword"
      },
      "method": {
        "type": "keyword"
      },
      "url": {
        "type": "text",
        "analyzer": "audit_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "wildcard": {
            "type": "wildcard"
          }
        }
      },
      "statusCode": {
        "type": "keyword"
      },
      "requestBody": {
        "type": "text",
        "analyzer": "audit_analyzer"
      },
      "responseBody": {
        "type": "text",
        "analyzer": "audit_analyzer"
      }
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "audit_analyzer": {
          "type": "standard",
          "stopwords": "_english_"
        }
      }
    }
  }
}'

echo -e "\n"

curl -X PUT "$ES_HOST/audit-errors" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "timestamp": {
        "type": "date"
      },
      "errorType": {
        "type": "keyword"
      },
      "errorMessage": {
        "type": "text",
        "analyzer": "standard"
      },
      "originalTopic": {
        "type": "keyword"
      },
      "originalMessage": {
        "type": "text",
        "analyzer": "standard"
      },
      "processingStage": {
        "type": "keyword"
      }
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}'

echo "Elasticsearch indices created successfully!"

echo "Current indices:"
curl -X GET "$ES_HOST/_cat/indices/audit-*?v"