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
    <version>0.0.3</version>
</dependency>
```

## Usage

```java
// Instantiate tracer
Tracer tracer = ...

// Build TransportClient with TracingPreBuiltTransportClient
TransportClient transportClient = new TracingPreBuiltTransportClient(settings)
                .addTransportAddress(...));

// Build RestClient adding TracingHttpClientConfigCallback
 RestClient restClient = RestClient.builder(
                new HttpHost(...))
                .setHttpClientConfigCallback(new TracingHttpClientConfigCallback(tracer))
                .build();


```

[ci-img]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-elasticsearch-client.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-elasticsearch-client
