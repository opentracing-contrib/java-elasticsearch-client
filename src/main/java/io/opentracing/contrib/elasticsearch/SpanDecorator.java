package io.opentracing.contrib.elasticsearch;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.elasticsearch.action.ActionResponse;

class SpanDecorator {

  static final String COMPONENT_NAME = "java-elasticsearch";

  static void onRequest(Span span) {
    Tags.COMPONENT.set(span, COMPONENT_NAME);
  }

  static void onRequest(HttpRequest request, Span span) {
    Tags.COMPONENT.set(span, COMPONENT_NAME);
    Tags.HTTP_METHOD.set(span, request.getRequestLine().getMethod());
    Tags.HTTP_URL.set(span, request.getRequestLine().getUri());
  }

  static void onResponse(ActionResponse response, Span span) {
    if (response.remoteAddress() != null) {
      Tags.PEER_HOSTNAME.set(span, response.remoteAddress().getHost());
    }
  }

  static void onResponse(HttpResponse response, Span span) {
    Tags.HTTP_STATUS.set(span, response.getStatusLine().getStatusCode());
  }

  static void onError(Throwable throwable, Span span) {
    Tags.ERROR.set(span, Boolean.TRUE);
    span.log(errorLogs(throwable));
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(4);
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.kind", throwable.getClass().getName());
    errorLogs.put("error.object", throwable);

    errorLogs.put("message", throwable.getMessage());

    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    errorLogs.put("stack", sw.toString());

    return errorLogs;
  }
}

