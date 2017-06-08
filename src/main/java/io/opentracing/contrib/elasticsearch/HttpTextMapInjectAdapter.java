package io.opentracing.contrib.elasticsearch;


import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.http.HttpRequest;

public class HttpTextMapInjectAdapter implements TextMap {

  private final HttpRequest httpRequest;

  public HttpTextMapInjectAdapter(HttpRequest request) {
    this.httpRequest = request;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    throw new UnsupportedOperationException("iterator should never be used with Tracer.inject()");
  }

  @Override
  public void put(String key, String value) {
    httpRequest.addHeader(key, value);
  }
}
