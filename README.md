[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Elasticsearch Client Instrumentation
OpenTracing instrumentation for Elasticsearch clients.

## Installation

### Maven
pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-elasticsearch-client</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Usage

`DefaultSpanManager` is used to get active span

```java
// Instantiate tracer
Tracer tracer = ...

// Register tracer with GlobalTracer
GlobalTracer.register(tracer);


// Build TransportClient with TracingPreBuiltTransportClient
TransportClient transportClient = new TracingPreBuiltTransportClient(settings)
                .addTransportAddress(...));

// Build RestClient adding TracingHttpClientConfigCallback
 RestClient restClient = RestClient.builder(
                new HttpHost(...))
                .setHttpClientConfigCallback(new TracingHttpClientConfigCallback())
                .build();


```

[ci-img]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-elasticsearch-client.svg?maxAge=2592000
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-elasticsearch-client
