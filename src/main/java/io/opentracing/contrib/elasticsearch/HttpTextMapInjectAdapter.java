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
