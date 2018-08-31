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
package io.opentracing.contrib.elasticsearch6;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.elasticsearch.common.SpanDecorator;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.util.Collection;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


public class TracingPreBuiltTransportClient extends PreBuiltTransportClient {

  private final Tracer tracer;

  @SafeVarargs
  public TracingPreBuiltTransportClient(Tracer tracer, Settings settings,
      Class<? extends Plugin>... plugins) {
    super(settings, plugins);
    this.tracer = tracer;
  }

  /**
   * GlobalTracer is used to get tracer
   */
  @SafeVarargs
  public TracingPreBuiltTransportClient(Settings settings,
      Class<? extends Plugin>... plugins) {
    this(GlobalTracer.get(), settings, plugins);
  }

  public TracingPreBuiltTransportClient(Tracer tracer, Settings settings,
      Collection<Class<? extends Plugin>> plugins) {
    super(settings, plugins);
    this.tracer = tracer;
  }

  /**
   * GlobalTracer is used to get tracer
   */
  public TracingPreBuiltTransportClient(Settings settings,
      Collection<Class<? extends Plugin>> plugins) {
    this(GlobalTracer.get(), settings, plugins);
  }

  public TracingPreBuiltTransportClient(Tracer tracer, Settings settings,
      Collection<Class<? extends Plugin>> plugins,
      HostFailureListener hostFailureListener) {
    super(settings, plugins, hostFailureListener);
    this.tracer = tracer;
  }

  /**
   * GlobalTracer is used to get tracer
   */
  public TracingPreBuiltTransportClient(Settings settings,
      Collection<Class<? extends Plugin>> plugins,
      HostFailureListener hostFailureListener) {
    this(GlobalTracer.get(), settings, plugins, hostFailureListener);
  }

  @Override
  protected <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(
      Action<Request, Response, RequestBuilder> action, Request request,
      ActionListener<Response> listener) {
    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(request.getClass().getSimpleName())
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

    Span span = spanBuilder.start();
    SpanDecorator.onRequest(span);

    ActionListener<Response> actionFuture = new TracingResponseListener<>(listener, span);
    super.doExecute(action, request, actionFuture);
  }
}
