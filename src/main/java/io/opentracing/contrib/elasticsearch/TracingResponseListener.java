package io.opentracing.contrib.elasticsearch;

import io.opentracing.Span;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;

public class TracingResponseListener<T extends ActionResponse> implements ActionListener<T> {
    private final ActionListener<T> listener;
    private final Span span;

    public TracingResponseListener(ActionListener<T> listener, Span span) {
        this.listener = listener;
        this.span = span;
    }

    @Override
    public void onResponse(T t) {
        SpanDecorator.onResponse(t, span);

        try {
            listener.onResponse(t);
        } finally {
            span.finish();
        }
    }

    @Override
    public void onFailure(Exception e) {
        SpanDecorator.onError(e, span);

        try {
            listener.onFailure(e);
        } finally {
            span.finish();
        }
    }
}
