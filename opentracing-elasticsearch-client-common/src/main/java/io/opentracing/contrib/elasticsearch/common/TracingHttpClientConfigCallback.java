/*
 * Copyright 2017-2019 The OpenTracing Authors
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
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.lang.reflect.Field;
import java.util.function.Function;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;


public class TracingHttpClientConfigCallback implements RestClientBuilder.HttpClientConfigCallback {

  private final Tracer tracer;
  private final Function<HttpRequest, String> spanNameProvider;
  private final HttpClientConfigCallback callback;
  private static final String OT_IS_AUTH_CACHING_DISABLED = "ot-is-auth-caching-disabled";
  private static final String OT_SPAN = "ot-span";

  public TracingHttpClientConfigCallback(Tracer tracer,
      Function<HttpRequest, String> spanNameProvider,
      HttpClientConfigCallback callback) {
    this.tracer = tracer;
    this.spanNameProvider = spanNameProvider;
    this.callback = callback;
  }

  public TracingHttpClientConfigCallback(Tracer tracer,
      Function<HttpRequest, String> spanNameProvider) {
    this(tracer, spanNameProvider, null);
  }

  /**
   * Default span name provider (ClientSpanNameProvider.REQUEST_METHOD_NAME) is used
   */
  public TracingHttpClientConfigCallback(Tracer tracer) {
    this(tracer, ClientSpanNameProvider.REQUEST_METHOD_NAME, null);
  }

  /**
   * Default span name provider (ClientSpanNameProvider.REQUEST_METHOD_NAME) is used
   */
  public TracingHttpClientConfigCallback(Tracer tracer, HttpClientConfigCallback callback) {
    this(tracer, ClientSpanNameProvider.REQUEST_METHOD_NAME, callback);
  }

  /**
   * GlobalTracer is used to get tracer. Default span name provider (ClientSpanNameProvider.REQUEST_METHOD_NAME)
   * is used
   */
  public TracingHttpClientConfigCallback() {
    this(GlobalTracer.get(), ClientSpanNameProvider.REQUEST_METHOD_NAME, null);
  }

  /**
   * GlobalTracer is used to get tracer. Default span name provider (ClientSpanNameProvider.REQUEST_METHOD_NAME)
   * is used
   */
  public TracingHttpClientConfigCallback(HttpClientConfigCallback callback) {
    this(GlobalTracer.get(), ClientSpanNameProvider.REQUEST_METHOD_NAME, callback);
  }

  private boolean isAuthCachingDisabled(HttpAsyncClientBuilder httpAsyncClientBuilder) {
    try {
      final Field authCachingDisabledField = httpAsyncClientBuilder.getClass()
          .getDeclaredField("authCachingDisabled");
      final boolean accessible = authCachingDisabledField.isAccessible();
      authCachingDisabledField.setAccessible(true);
      final boolean isAuthCachingDisabled = (boolean) authCachingDisabledField
          .get(httpAsyncClientBuilder);
      authCachingDisabledField.setAccessible(accessible);
      return isAuthCachingDisabled;
    } catch (Exception ignore) {
    }
    return false;
  }

  @Override
  public HttpAsyncClientBuilder customizeHttpClient(
      final HttpAsyncClientBuilder httpAsyncClientBuilder) {

    HttpAsyncClientBuilder httpClientBuilder;
    final boolean isAuthCachingDisabled;
    if (callback != null) {
      httpClientBuilder = callback.customizeHttpClient(httpAsyncClientBuilder);
      isAuthCachingDisabled = isAuthCachingDisabled(httpClientBuilder);
    } else {
      httpClientBuilder = httpAsyncClientBuilder;
      isAuthCachingDisabled = false;
    }

    httpClientBuilder.addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
      SpanBuilder spanBuilder = tracer.buildSpan(spanNameProvider.apply(request))
          .ignoreActiveSpan()
          .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

      SpanContext parentContext = extract(request);

      if (parentContext != null) {
        spanBuilder.asChildOf(parentContext);
      }

      Span span = spanBuilder.start();
      SpanDecorator.onRequest(request, span);

      tracer.inject(span.context(), Builtin.HTTP_HEADERS,
          new HttpTextMapInjectAdapter(request));

      context.setAttribute(OT_SPAN, span);
      if (isAuthCachingDisabled) {
        context.setAttribute(OT_IS_AUTH_CACHING_DISABLED, "true");
      }
    });

    httpClientBuilder.addInterceptorFirst((HttpResponseInterceptor) (response, context) -> {
      if (context.getAttribute(OT_IS_AUTH_CACHING_DISABLED) != null) {
        context.removeAttribute(OT_IS_AUTH_CACHING_DISABLED);
        if (response.getStatusLine().getStatusCode() == 401) {
          // response interceptor is called twice if auth caching is disabled
          // and server requires authentication
          return;
        }
      }
      Object spanObject = context.getAttribute(OT_SPAN);
      if (spanObject instanceof Span) {
        context.removeAttribute(OT_SPAN);
        Span span = (Span) spanObject;
        SpanDecorator.onResponse(response, span);
        span.finish();
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
