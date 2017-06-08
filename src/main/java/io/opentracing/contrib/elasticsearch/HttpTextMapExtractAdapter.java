package io.opentracing.contrib.elasticsearch;

import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpRequest;


public class HttpTextMapExtractAdapter implements TextMap {

  private final Map<String, String> map = new HashMap<>();

  public HttpTextMapExtractAdapter(HttpRequest request) {
    for (Header header : request.getAllHeaders()) {
      map.put(header.getName(), header.getValue());
    }
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return map.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    throw new UnsupportedOperationException(
        "HttpTextMapExtractAdapte should only be used with Tracer.extract()");
  }
}
