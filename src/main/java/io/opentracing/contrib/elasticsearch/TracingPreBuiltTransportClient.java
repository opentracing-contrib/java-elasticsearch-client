package io.opentracing.contrib.elasticsearch;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
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

  public TracingPreBuiltTransportClient(Tracer tracer, Settings settings,
      Collection<Class<? extends Plugin>> plugins) {
    super(settings, plugins);
    this.tracer = tracer;
  }

  public TracingPreBuiltTransportClient(Tracer tracer, Settings settings,
      Collection<Class<? extends Plugin>> plugins,
      HostFailureListener hostFailureListener) {
    super(settings, plugins, hostFailureListener);
    this.tracer = tracer;
  }

  @Override
  protected <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(
      Action<Request, Response, RequestBuilder> action, Request request,
      ActionListener<Response> listener) {
    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(request.getClass().getSimpleName())
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

    Span span = spanBuilder.startManual();
    SpanDecorator.onRequest(span);

    ActionListener<Response> actionFuture = new TracingResponseListener<>(listener, span);
    super.doExecute(action, request, actionFuture);
  }
}
