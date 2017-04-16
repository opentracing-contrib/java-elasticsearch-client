package io.opentracing.contrib.elasticsearch;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;


public class TracingHttpClientConfigCallback implements RestClientBuilder.HttpClientConfigCallback {

    @Override
    public HttpAsyncClientBuilder customizeHttpClient(final HttpAsyncClientBuilder httpClientBuilder) {

        httpClientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(request.getRequestLine().getMethod())
                        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

                SpanContext parentContext = extract(request);

                if (parentContext != null) {
                    spanBuilder.asChildOf(parentContext);
                }

                Span span = spanBuilder.start();
                SpanDecorator.onRequest(request, span);

                GlobalTracer.get().inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpTextMapInjectAdapter(request));

                context.setAttribute("span", span);
            }
        });

        httpClientBuilder.addInterceptorFirst(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                Object spanObject = context.getAttribute("span");
                if (spanObject != null && spanObject instanceof Span) {
                    Span span = (Span) spanObject;
                    SpanDecorator.onResponse(response, span);
                    span.finish();
                }
            }
        });

        return httpClientBuilder;
    }

    /**
     * Extract context from headers or from Span Manager
     *
     * @param request http request
     * @return extracted context
     */
    private SpanContext extract(HttpRequest request) {
        SpanContext spanContext = GlobalTracer.get().extract(Format.Builtin.HTTP_HEADERS,
                new HttpTextMapExtractAdapter(request));

        if (spanContext != null) {
            return spanContext;
        }

        Span span = DefaultSpanManager.getInstance().current().getSpan();
        if (span != null) {
            return span.context();
        }

        return null;
    }
}
