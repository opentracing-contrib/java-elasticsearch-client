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

import static org.junit.Assert.assertEquals;

import java.util.function.Function;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;

public class ClientSpanNameProviderTest {

  /**
   * The following requests demonstrate the API from https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs.html
   */

  // https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-get.html
  private final static HttpRequest getRequest = RequestBuilder.create("GET")
      .setUri("/twitter/tweet/")
      .build();
  private final static HttpRequest getRequestWithID = RequestBuilder.create("GET")
      .setUri("/twitter/tweet/1")
      .build();
  private final static HttpRequest getIndexRequestWithIDAndParameters = RequestBuilder.create("GET")
      .setUri("/twitter/tweet/1?routing=user1")
      .build();
  private final static HttpRequest getRequestWithIDWithSource = RequestBuilder.create("GET")
      .setUri("/twitter/tweet/1/_source")
      .build();

  // https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-delete-by-query.html#docs-delete-by-query-cancel-task-api
  private final static HttpRequest postRequestCancelTaskID = RequestBuilder.create("POST")
      .setUri("/_tasks/task_id:1/_cancel")
      .build();

  @Test
  public void requestMethodSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider.REQUEST_METHOD_NAME;

    assertEquals("GET", spanNameProvider.apply(getRequest));
    assertEquals("GET", spanNameProvider.apply(getRequestWithID));
    assertEquals("GET", spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("GET", spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("POST", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestMethodSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_METHOD_NAME("ELASTICSEARCH - ");

    assertEquals("ELASTICSEARCH - GET", spanNameProvider.apply(getRequest));
    assertEquals("ELASTICSEARCH - GET", spanNameProvider.apply(getRequestWithID));
    assertEquals("ELASTICSEARCH - GET", spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("ELASTICSEARCH - GET", spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("ELASTICSEARCH - POST", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestMethodSpanNameHandlesNull() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_METHOD_NAME(null);

    assertEquals("GET", spanNameProvider.apply(getRequest));
    assertEquals("GET", spanNameProvider.apply(getRequestWithID));
    assertEquals("GET", spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("GET", spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("POST", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void requestTargetSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider.REQUEST_TARGET_NAME;

    assertEquals("/twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("/twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("/twitter/tweet/?", spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("/twitter/tweet/?/_source", spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("/_tasks/task_id:?/_cancel", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestTargetSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_TARGET_NAME("ELASTICSEARCH - ");

    assertEquals("ELASTICSEARCH - /twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("ELASTICSEARCH - /twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("ELASTICSEARCH - /twitter/tweet/?",
        spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("ELASTICSEARCH - /twitter/tweet/?/_source",
        spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("ELASTICSEARCH - /_tasks/task_id:?/_cancel",
        spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestTargetSpanNameHandlesNull() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_TARGET_NAME(null);

    assertEquals("/twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("/twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("/twitter/tweet/?", spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("/twitter/tweet/?/_source", spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("/_tasks/task_id:?/_cancel", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void requestMethodTargetSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider.REQUEST_METHOD_TARGET_NAME;

    assertEquals("GET /twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("GET /twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("GET /twitter/tweet/?",
        spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("GET /twitter/tweet/?/_source",
        spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("POST /_tasks/task_id:?/_cancel", spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestMethodTargetSpanNameFormatsCorrectly() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_METHOD_TARGET_NAME("ELASTICSEARCH - ");

    assertEquals("ELASTICSEARCH - GET /twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("ELASTICSEARCH - GET /twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("ELASTICSEARCH - GET /twitter/tweet/?",
        spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("ELASTICSEARCH - GET /twitter/tweet/?/_source",
        spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("ELASTICSEARCH - POST /_tasks/task_id:?/_cancel",
        spanNameProvider.apply(postRequestCancelTaskID));
  }

  @Test
  public void prefixedRequestMethodTargetSpanNameHandlesNull() {
    Function<HttpRequest, String> spanNameProvider = ClientSpanNameProvider
        .PREFIXED_REQUEST_METHOD_TARGET_NAME(null);

    assertEquals("GET /twitter/tweet/", spanNameProvider.apply(getRequest));
    assertEquals("GET /twitter/tweet/?", spanNameProvider.apply(getRequestWithID));
    assertEquals("GET /twitter/tweet/?",
        spanNameProvider.apply(getIndexRequestWithIDAndParameters));
    assertEquals("GET /twitter/tweet/?/_source",
        spanNameProvider.apply(getRequestWithIDWithSource));

    assertEquals("POST /_tasks/task_id:?/_cancel", spanNameProvider.apply(postRequestCancelTaskID));
  }

}
