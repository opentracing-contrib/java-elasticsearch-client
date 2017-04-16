package io.opentracing.contrib.elasticsearch;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.util.Collection;


public class TracingPreBuiltTransportClient extends PreBuiltTransportClient {

    @SafeVarargs
    public TracingPreBuiltTransportClient(Settings settings, Class<? extends Plugin>... plugins) {
        super(settings, plugins);
    }

    public TracingPreBuiltTransportClient(Settings settings, Collection<Class<? extends Plugin>> plugins) {
        super(settings, plugins);
    }

    public TracingPreBuiltTransportClient(Settings settings, Collection<Class<? extends Plugin>> plugins,
                                          HostFailureListener hostFailureListener) {
        super(settings, plugins, hostFailureListener);
    }

    @Override
    protected <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> void doExecute(Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {
        Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(request.getClass().getSimpleName())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        Span parentSpan = DefaultSpanManager.getInstance().current().getSpan();
        if (parentSpan != null) {
            spanBuilder.asChildOf(parentSpan.context());
        }

        Span span = spanBuilder.start();
        SpanDecorator.onRequest(span);

        ActionListener<Response> actionFuture = new TracingResponseListener<>(listener, span);
        super.doExecute(action, request, actionFuture);
    }
}
