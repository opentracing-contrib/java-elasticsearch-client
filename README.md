[![Build Status][ci-img]][ci] [![Coverage Status][cov-img]][cov] [![Released Version][maven-img]][maven] [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# OpenTracing Elasticsearch Client Instrumentation
OpenTracing instrumentation for Elasticsearch clients.

## Installation

### Maven
pom.xml

#### Elasticsearch 5

```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-elasticsearch5-client</artifactId>
    <version>VERSION</version>
</dependency>
```

#### Elasticsearch 6

```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-elasticsearch6-client</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

```java
// Instantiate tracer
Tracer tracer = ...

// Optionally register tracer with GlobalTracer
GlobalTracer.register(tracer);

// Build TransportClient with TracingPreBuiltTransportClient
TransportClient transportClient = new TracingPreBuiltTransportClient(settings)
                .addTransportAddress(...));

// Build RestClient adding TracingHttpClientConfigCallback
 RestClient restClient = RestClient.builder(
                new HttpHost(...))
                .setHttpClientConfigCallback(new TracingHttpClientConfigCallback(tracer))
                .build();


```

## Custom Span Names with the TracingHttpClientConfigCallback
This driver includes support for customizing the spans created using the TracingHttpClientConfigCallback.
You can use the predefined ones listed further below, or write your own in the form of a `Function` object.
```java
// Create a Function for the TracingHttpClientConfigCallback that operates on
// the HttpRequest and returns a String that will be used as the Span name.
Function<HttpRequest, String> customSpanNameProvider =
  (request) -> request.getRequestLine().getMethod().toLowerCase();

// Build RestClient adding TracingHttpClientConfigCallback
 RestClient restClient = RestClient.builder(
                new HttpHost(...))
                .setHttpClientConfigCallback(new TracingHttpClientConfigCallback(tracer, customSpanNameProvider))
                .build();
 
 // Spans created by the restClient will now have the request's lowercase method name as the span name.
 // "POST" -> "post"
```

### Predefined Span Name Providers 
The following Functions are already included in the ClientSpanNameProvider class, with `REQUEST_METHOD_NAME` being the
default should no other span name provider be provided.

* `REQUEST_METHOD_NAME`: Returns the HTTP method of the request.
  * GET /twitter/tweet/1?routing=user1 -> "GET"
* `PREFIXED_REQUEST_METHOD_NAME(String prefix)`: Returns a String concatenation of prefix and the HTTP method of the request.
  * GET /twitter/tweet/1?routing=user1 -> prefix + "GET"
* `REQUEST_TARGET_NAME`: Returns the Elasticsearch target of the request, i.e. the index and resource it's operating on.
IDs and other numbers not part of names will be replaced with a "?" to avoid overly granular names.
  * GET /twitter/tweet/1?routing=user1 -> "/twitter/tweet/?"
* `PREFIXED_REQUEST_TARGET_NAME(String prefix)`: Returns a String concatenation of prefix and the Elasticsearch target of the request.
IDs and other numbers not part of names will be replaced with a "?" to avoid overly granular names.
  * GET /twitter/tweet/1?routing=user1 -> prefix + "/twitter/tweet/?"
* `REQUEST_METHOD_TARGET_NAME`: Returns a String concatenation of the HTTP method of the request and the Elasticsearch target of the request.
IDs and other numbers not part of names will be replaced with a "?" to avoid overly granular names.
  * GET /twitter/tweet/1?routing=user1 -> "GET /twitter/tweet/?"
* `PREFIXED_REQUEST_METHOD_TARGET_NAME(String prefix)`: Returns a String concatenation of prefix, the HTTP method of the request, and
the Elasticsearch target of the request. IDs and other numbers not part of names will be replaced with a "?" to avoid overly granular names.
  * GET /twitter/tweet/1?routing=user1 -> prefix + "GET /twitter/tweet/?"
  
## License

[Apache 2.0 License](./LICENSE).

[ci-img]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-elasticsearch-client
[cov-img]: https://coveralls.io/repos/github/opentracing-contrib/java-elasticsearch-client/badge.svg?branch=master
[cov]: https://coveralls.io/github/opentracing-contrib/java-elasticsearch-client?branch=master
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-elasticsearch6-client.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-elasticsearch6-client
