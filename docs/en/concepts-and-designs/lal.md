# Log Analysis Language

Log Analysis Language (LAL) in SkyWalking is essentially a Domain-Specific Language (DSL) to analyze logs. You can use
LAL to parse, extract, filter, analyze, and save the logs, as well as collaborate the logs with traces, and metrics.

Check the [`default.yaml`](../../../oap-server/server-bootstrap/src/main/resources/lal/default.yaml) for examples.

## Filter

A filter is a group of [parser](#parser), [extractor](#extractor) and [sink](#sink). Users can use one or more filters
to organize their processing logics. Every piece of log will be sent to all filters in an LAL rule. The piece of log sent into the filter is available as property `log` in the LAL, therefore you can access the log service name via `log.service`.

### Parser

Parsers are responsible for parsing the raw logs into structured data in SkyWalking for further processing. There are 3
types of parsers at the moment, namely `json`, `yaml`, and `text`.

When a piece of log is parsed, there is a corresponding property available, called `parsed`, injected by LAL.
Property `parsed` is typically a map, containing all the fields parsed from the raw logs, for example, if the parser
is `json` / `yaml`, `parsed` is a map containing all the key-values in the `json` / `yaml`, if the parser is `text`
, `parsed` is a map containing all the captured groups and their values (for `regexp` and `grok`). See examples below.

#### `json`

<!-- TODO: is structured in the reported (gRPC) `LogData`, not much to do -->

#### `yaml`

<!-- TODO: is structured in the reported (gRPC) `LogData`, not much to do -->

#### `text`

For unstructured logs, there are some `text` parsers for use.

- `regexp`

`regexp` parser uses a regular expression (`regexp`) to parse the logs. It leverages the captured groups of the regexp,
all the captured groups can be used later in the extractors or sinks.

```groovy
filter {
    text {
        regexp "(?<timestamp>\\d{8}) (?<thread>\\w+) (?<level>\\w+) (?<traceId>\\w+) (?<msg>.+)"
        // this is just a demo pattern
    }
    extractor {
        tag key: "level", val: parsed["level"]
        // we add a tag called `level` and its value is parsed["level"], captured from the regexp above
        traceId parsed["traceId"]
        // we also extract the trace id from the parsed result, which will be used to associate the log with the trace
    }
    // ...
}
```

- `grok`

<!-- TODO: grok Java library has poor performance, need to benchmark it, the idea is basically the same with `regexp` above -->

### Extractor

Extractors aim to extract metadata from the logs. The metadata can be a service name, a service instance name, an
endpoint name, or even a trace ID, all of which can be associated with the existing traces and metrics.

- `service`

`service` extracts the service name from the `parsed` result, and set it into the `LogData`, which will be persisted (if
not dropped) and is used to associate with traces / metrics.

- `instance`

`instance` extracts the service instance name from the `parsed` result, and set it into the `LogData`, which will be
persisted (if not dropped) and is used to associate with traces / metrics.

- `endpoint`

`endpoint` extracts the service instance name from the `parsed` result, and set it into the `LogData`, which will be
persisted (if not dropped) and is used to associate with traces / metrics.

- `traceId`

`traceId` extracts the trace ID from the `parsed` result, and set it into the `LogData`, which will be
persisted (if not dropped) and is used to associate with traces / metrics.

- `segmentId`

`segmentId` extracts the segment ID from the `parsed` result, and set it into the `LogData`, which will be
persisted (if not dropped) and is used to associate with traces / metrics.

- `spanId`

`spanId` extracts the span ID from the `parsed` result, and set it into the `LogData`, which will be
persisted (if not dropped) and is used to associate with traces / metrics.

- `metrics`

`metrics` extracts / generates metrics from the logs. The supported metrics are `CounterMetrics`, `GaugeMetrics`,
and `HistogramMetrics`, examples are as follows:

```groovy
filter {
    // ...
    extractor {
        service parsed["serviceName"]
        metrics {
            counter { // whenever the filter is fed with a piece of log, the counter will be increased by one
                name: "logsCount"
                tips: "The total count of received logs"
                tags: ["key1": "value1", "key2": "value2"]
            }
            if (parsed["level"] == "ERROR") {
                counter {
                    // whenever the filter is fed with a piece of log whose level is ERROR, the counter will be increased by one
                    name: "errorLogsCount"
                    tips: "The total count of error logs"
                    tags:
                    ["key1": "value1", "key2": "value2"]
                }
            }
            gauge { // whenever the filter is fed with a piece of log, the gauge will be increased by one
                name: "whatever"
                tips: "whatever"
                tags: ["k1": "v1", "k2": "v2"]
            }
            histogram {
                name: "httpRequestTime"
                tips: "The time elapsed of the http request"
                startNanos: parsed["timestamp"]
                duration: parsed["duration"]
            }
        }
    }
    // ...
}
```

### Sink

Sinks are the persistent layer of the LAL. By default, all the logs of each filter are persisted into the storage. However, there are some mechanisms that allow you to selectively save some logs, or even drop all the logs after you've extracted useful information, such as metrics.

#### Sampler

Sampler allows you to save the logs in a sampling manner. Currently, 2 sampling strategies are supported, `ratelimit` and `probabilistic`.

`ratelimit` samples `n` logs at most, in a given duration (e.g. 1 second).
`probabilistic` samples `n%` logs .

Examples:

```groovy
filter {
    // ... parser
    
    sampler {
        ratelimit 100.per.second // 100 logs per second
    }
}
```

```groovy
filter {
    // ... parser
    
    sampler {
        probabilistic 50.percent // 50% logs
    }
}
```

#### Dropper

Dropper is a special sink, meaning that all the logs are dropped without any exception. This is useful when you want to drop debugging logs,

```groovy
filter {
    // ... parser
    
    sink {
        if (parsed.level == "DEBUG") {
            dropper {}
        } else {
            sampler {
                // ... configs
            }
        }
    }
}
```

or you have multiple filters, some of which are for extracting metrics, only one of them needs to be persisted.

```groovy
filter { // filter A: this is for persistence
    // ... parser

    sink {
        sampler {
            // .. sampler configs
        }
    }
}
filter { // filter B:
    // ... extractors to generate many metrics
    extractors {
        metrics {
            // ... counter
            // ... gauge
            // ... histogram
            // ... etc.
        }
    }
    sink {
        dropper {} // drop all logs because they have been saved in "filter A" above.
    }
}
```
