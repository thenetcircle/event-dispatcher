const sourceSchemas: any = {
  'http': {
    "title": "Http Source",
    "type": "object",
    "properties": {
      "interface": {
        "type": "string",
        "default": "0.0.0.0"
      },
      "port": {
        "type": "integer",
        "default": 8000,
        "minimum": 1024,
        "maximum": 65535,
        "required": true
      },
      "format": {
        "type": "string",
        "enum": [
          "ActivityStreams"
        ]
      },
      "succeeded-response": {
        "type": "string",
        "default": "ok"
      },
      "server": {
        "type": "object",
        "title": "Server",
        "properties": {
          "max-connections": {
            "type": "integer",
            "default": 1024
          },
          "request-timeout": {
            "type": "string",
            "default": "10 s",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          },
          "idle-timeout": {
            "type": "string",
            "default": "60 s",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          },
          "bind-timeout": {
            "type": "string",
            "default": "1 s",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          },
          "linger-timeout": {
            "type": "string",
            "default": "1 min",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          }
        }
      }
    }
  },

  'kafka': {
    "title": "Kafka Source",
    "type": "object",
    "properties": {
      "bootstrap-servers": {
        "type": "string",
        "title": "Bootstrap Servers",
        "default": "",
        "required": true
      },
      "group-id": {
        "type": "string",
        "title": "Group Id",
        "default": ""
      },
      "topics": {
        "type": "array",
        "title": "Subscribed Topics",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "topic"
        },
        "required": true
      },
      "topic-pattern": {
        "type": "string",
        "title": "Subscribed Topic Pattern",
        "description": "supports regex pattern, if \"Subscribed Topics\" is set, will use it instead",
        "default": "",
        "required": true
      },
      "max-concurrent-partitions": {
        "type": "integer",
        "default": 1024
      },
      "commit-max-batches": {
        "type": "integer",
        "default": 50
      },
      "akka-kafka": {
        "type": "object",
        "title": "Akka Kafka Configuration",
        "properties": {
          "use-dispatcher": {
            "type": "string",
            "default": "akka.kafka.default-dispatcher"
          },
          "poll-interval": {
            "type": "string",
            "default": "50ms"
          },
          "poll-timeout": {
            "type": "string",
            "default": "50ms"
          },
          "stop-timeout": {
            "type": "string",
            "default": "30s"
          },
          "close-timeout": {
            "type": "string",
            "default": "20s"
          },
          "commit-timeout": {
            "type": "string",
            "default": "15s"
          },
          "commit-time-warning": {
            "type": "string",
            "default": "1s"
          },
          "wakeup-timeout": {
            "type": "string",
            "default": "3s"
          },
          "max-wakeups": {
            "type": "integer",
            "default": 10
          },
          "wait-close-partition": {
            "type": "string",
            "default": "500ms"
          },
          "wakeup-debug": {
            "type": "boolean",
            "default": true
          }
        }
      },
      "properties": {
        "type": "object",
        "title": "Kafka Consumer Properties",
        "properties": {}
      }
    }
  }
}

const operatorSchemas: any = {
  'tnc-topic-resolver': {
    "title": "TNC Topic Resolver Transform",
    "type": "object",
    "properties": {
      "default-topic": {
        "type": "string",
        "title": "Default Topic",
        "description": "supports substitutes: {app_name}, {app_env}, {story_name}",
        "default": "event-v2-{app_name}{app_env}-default"
      }
    }
  },
  'filter': {
    "title": "TNC Event Filter",
    "type": "object",
    "properties": {
      "event-name-white-list": {
        "type": "array",
        "title": "Event Name White List",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "Event Name Regex"
        }
      },
      "event-name-black-list": {
        "type": "array",
        "title": "Event Name Black List",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "Event Name Regex"
        }
      },
      "channel-white-list": {
        "type": "array",
        "title": "Channel White List",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "Channel Regex"
        }
      },
      "channel-black-list": {
        "type": "array",
        "title": "Channel Black List",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "Channel Regex"
        }
      },
      "allowed-transport-modes": {
        "type": "array",
        "title": "Allowed Transport Modes",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "enum": [
            "SYNC_PLUS", "ASYNC", "BOTH", "NONE"
          ],
          "title": "Transport Mode"
        },
        "default": ["ASYNC", "BOTH", "NONE"]
      },
      "only-extras": {
        "type": "object",
        "title": "Only Matched Extra Infos",
        "properties": {}
      }
    }
  },
  'cassandra': {
    "type": "object",
    "properties": {
      "contact-points": {
        "type": "array",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "title": "host"
        },
        "required": true
      },
      "port": {
        "type": "integer",
        "default": 9042,
        "required": true
      },
      "parallelism": {
        "type": "integer",
        "default": 3
      }
    }
  },
  'decoupler': {
    "type": "object",
    "direction": "bidi",
    "properties": {
      "buffer-size": {
        "type": "integer",
        "default": 10000
      },
      "terminate-delay": {
        "type": "string",
        "default": "10 m",
        "description": "possible units: (s)econd, (m)inute, (h)ours"
      },
      "secondary-sink": {
        "type": "string",
        "description": "configuration of the failover task",
        "required": true
      },
      "secondary-sink-buffer-size": {
        "type": "integer",
        "default": 1000
      }
    }
  },
  'file': {
    "type": "object",
    "properties": {
      "path": {
        "type": "string",
        "required": true
      },
      "line-delimiter": {
        "type": "string",
        "default": "<tab>"
      },
      "event-delimiter": {
        "type": "string",
        "default": "<newline>#-:#:-#<newline>"
      }
    }
  }
}

const sinkSchemas: any = {
  'http': {
    "title": "Http Sink",
    "type": "object",
    "properties": {
      "default-request": {
        "type": "object",
        "title": "Server",
        "properties": {
          "method": {
            "type": "string",
            "enum": [
              "POST", "GET", "PUT", "OPTIONS", "DELETE", "HEAD", "PATCH"
            ],
            "required": true
          },
          "uri": {
            "type": "string",
            "description": "when event does not include url info, use this default one",
            "required": true
          },
          "protocol": {
            "type": "string",
            "default": "HTTP/1.1"
          },
          "headers": {
            "type": "object",
            "title": "Headers",
            "properties": {}
          }
        },
        "required": true
      },
      "concurrent-requests": {
        "type": "integer",
        "default": 1
      },
      "request-buffer-size": {
        "type": "integer",
        "default": 100
      },
      "expected-response": {
        "type": "string",
        "default": "ok"
      },
      "allow-extra-signals": {
        "type": "boolean",
        "default": true
      },
      "content-type": {
        "type": "string",
        "default": "text"
      },
      "use-https-connection-pool": {
        "type": "boolean",
        "default": false
      },
      "use-retry-sender": {
        "type": "boolean",
        "default": true
      },
      "retry-sender": {
        "type": "object",
        "properties": {
          "min-backoff": {
            "type": "string",
            "default": "1 s"
          },
          "max-backoff": {
            "type": "string",
            "default": "30 s"
          },
          "random-factor": {
            "type": "number",
            "default": 0.2
          },
          "retry-duration": {
            "type": "string",
            "default": "12 h"
          }
        }
      },
      "pool": {
        "type": "object",
        "title": "Http Connection Pool",
        "properties": {
          "max-connections": {
            "type": "integer",
            "default": 32
          },
          "min-connections": {
            "type": "integer",
            "default": 0
          },
          "max-retries": {
            "type": "integer",
            "default": 1
          },
          "max-open-requests": {
            "type": "integer",
            "default": 256
          },
          "pipelining-limit": {
            "type": "integer",
            "default": 1
          },
          "idle-timeout": {
            "type": "string",
            "default": "30 s",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          }
        }
      }
    }
  },

  'firebase': {
    "title": "Firebase Sink",
    "type": "object",
    "properties": {
      "default-request": {
        "type": "object",
        "title": "Server",
        "properties": {
          "method": {
            "type": "string",
            "enum": [
              "POST", "GET", "PUT", "OPTIONS", "DELETE", "HEAD", "PATCH"
            ]
          },
          "uri": {
            "type": "string",
            "description": "when event does not include url info, use this default one",
            "required": true
          },
          "auth-key": {
            "type": "string",
            "title": "Authorization Key",
            "required": true
          },
          "protocol": {
            "type": "string",
            "default": "HTTP/1.1"
          },
          "headers": {
            "type": "object",
            "title": "Headers",
            "properties": {}
          }
        },
        "required": true
      },
      "concurrent-requests": {
        "type": "integer",
        "default": 1
      },
      "request-buffer-size": {
        "type": "integer",
        "default": 100
      },
      "expected-response": {
        "type": "string",
        "default": ""
      },
      "allow-extra-signals": {
        "type": "boolean",
        "default": false
      },
      "use-retry-sender": {
        "type": "boolean",
        "default": true
      },
      "retry-sender": {
        "type": "object",
        "properties": {
          "min-backoff": {
            "type": "string",
            "default": "1 s"
          },
          "max-backoff": {
            "type": "string",
            "default": "30 s"
          },
          "random-factor": {
            "type": "number",
            "default": 0.2
          },
          "retry-duration": {
            "type": "string",
            "default": "12 h"
          }
        }
      },
      "pool": {
        "type": "object",
        "title": "Http Connection Pool",
        "properties": {
          "max-connections": {
            "type": "integer",
            "default": 32
          },
          "min-connections": {
            "type": "integer",
            "default": 0
          },
          "max-retries": {
            "type": "integer",
            "default": 1
          },
          "max-open-requests": {
            "type": "integer",
            "default": 256
          },
          "pipelining-limit": {
            "type": "integer",
            "default": 1
          },
          "idle-timeout": {
            "type": "string",
            "default": "30 s",
            "description": "possible units: (s)econd, (m)inute, (h)ours"
          }
        }
      }
    }
  },

  'kafka': {
    "title": "Kafka Sink",
    "type": "object",
    "properties": {
      "bootstrap-servers": {
        "type": "string",
        "title": "Bootstrap Servers",
        "default": "",
        "required": true
      },
      "default-topic": {
        "type": "string",
        "default": "event-v2-{app_name}{app_env}-default"
      },
      "akka-kafka": {
        "type": "object",
        "title": "Akka Kafka Settings",
        "properties": {
          "parallelism": {
            "type": "integer",
            "default": 100
          },
          "close-timeout": {
            "type": "string",
            "default": "60 s"
          },
          "use-dispatcher": {
            "type": "string",
            "default": "akka.kafka.default-dispatcher"
          }
        }
      },
      "properties": {
        "type": "object",
        "title": "Kafka Producer Properties",
        "properties": {
          "acks": {
            "type": "string",
            "default": "all"
          },
          "retries": {
            "type": "integer",
            "default": 30
          },
          "max.in.flight.requests.per.connection": {
            "type": "integer",
            "default": 5
          },
          "enable.idempotence": {
            "type": "boolean",
            "default": true
          }
        }
      }
    }
  }
}


export default {
  'source': sourceSchemas,
  'operator': operatorSchemas,
  'sink': sinkSchemas
} as any
