/*
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.elasticsearch.common;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RestClientBuilder;


public class TracingHttpClientConfigCallback implements RestClientBuilder.HttpClientConfigCallback {

  private final Tracer tracer;

  public TracingHttpClientConfigCallback(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * GlobalTracer is used to get tracer
   */
  public TracingHttpClientConfigCallback() {
    this(GlobalTracer.get());
  }

  @Override
  public HttpAsyncClientBuilder customizeHttpClient(
      final HttpAsyncClientBuilder httpClientBuilder) {

    httpClientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {
      @Override
      public void process(HttpRequest request, HttpContext context)
          throws HttpException, IOException {
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(request.getRequestLine().getMethod())
            .ignoreActiveSpan()
            .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        SpanContext parentContext = extract(request);

        if (parentContext != null) {
          spanBuilder.asChildOf(parentContext);
        }

        Span span = spanBuilder.start();
        SpanDecorator.onRequest(request, span);

        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
            new HttpTextMapInjectAdapter(request));

        context.setAttribute("span", span);
      }
    });

    httpClientBuilder.addInterceptorFirst(new HttpResponseInterceptor() {
      @Override
      public void process(HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        Object spanObject = context.getAttribute("span");
        if (spanObject instanceof Span) {
          Span span = (Span) spanObject;
          SpanDecorator.onResponse(response, span);
          span.finish();
        }
      }
    });

    return httpClientBuilder;
  }

  /**
   * Extract context from headers or from active Span
   *
   * @param request http request
   * @return extracted context
   */
  private SpanContext extract(HttpRequest request) {
    SpanContext spanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
        new HttpTextMapExtractAdapter(request));

    if (spanContext != null) {
      return spanContext;
    }

    Span span = tracer.activeSpan();
    if (span != null) {
      return span.context();
    }

    return null;
  }
}
