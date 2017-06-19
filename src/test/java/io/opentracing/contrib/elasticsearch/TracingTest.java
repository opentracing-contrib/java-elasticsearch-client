package io.opentracing.contrib.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class TracingTest {

  private static final int HTTP_PORT = 9205;
  private static final String HTTP_TRANSPORT_PORT = "9305";
  private static final String ES_WORKING_DIR = "target/es";
  private static String clusterName = "cluster-name";
  private static Node node;
  private final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void startElasticsearch() throws Exception {
    Settings settings = Settings.builder()
        .put("path.home", ES_WORKING_DIR)
        .put("path.data", ES_WORKING_DIR + "/data")
        .put("path.logs", ES_WORKING_DIR + "/logs")
        .put("transport.type", "netty4")
        .put("http.type", "netty4")
        .put("cluster.name", clusterName)
        .put("http.port", HTTP_PORT)
        .put("transport.tcp.port", HTTP_TRANSPORT_PORT)
        .put("network.host", "127.0.0.1")
        .build();
    Collection plugins = Collections.singletonList(Netty4Plugin.class);
    node = new PluginConfigurableNode(settings, plugins);
    node.start();
  }

  @AfterClass
  public static void stopElasticsearch() throws Exception {
    node.close();
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void restClient() throws Exception {
    RestClient restClient = RestClient.builder(
        new HttpHost("localhost", HTTP_PORT, "http"))
        .setHttpClientConfigCallback(new TracingHttpClientConfigCallback(mockTracer))
        .build();

    HttpEntity entity = new NStringEntity(
        "{\n" +
            "    \"user\" : \"kimchy\",\n" +
            "    \"post_date\" : \"2009-11-15T14:12:12\",\n" +
            "    \"message\" : \"trying out Elasticsearch\"\n" +
            "}", ContentType.APPLICATION_JSON);

    Response indexResponse = restClient.performRequest(
        "PUT",
        "/twitter/tweet/1",
        Collections.<String, String>emptyMap(),
        entity);

    assertNotNull(indexResponse);

    final CountDownLatch latch = new CountDownLatch(1);
    restClient
        .performRequestAsync("PUT", "/twitter/tweet/2", Collections.<String, String>emptyMap(),
            entity, new ResponseListener() {
              @Override
              public void onSuccess(Response response) {
                latch.countDown();
              }

              @Override
              public void onFailure(Exception exception) {
                latch.countDown();
              }
            });

    latch.await(30, TimeUnit.SECONDS);
    restClient.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(2, finishedSpans.size());
    checkSpans(finishedSpans, "PUT");
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void transportClient() throws Exception {

    Settings settings = Settings.builder()
        .put("cluster.name", clusterName).build();

    TransportClient client = new TracingPreBuiltTransportClient(mockTracer, settings)
        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"),
            Integer.parseInt(HTTP_TRANSPORT_PORT)));

    IndexRequest indexRequest = new IndexRequest("twitter").type("tweet").id("1").
        source(jsonBuilder()
            .startObject()
            .field("user", "kimchy")
            .field("postDate", new Date())
            .field("message", "trying out Elasticsearch")
            .endObject()
        );

    IndexResponse indexResponse = client.index(indexRequest).actionGet();
    assertNotNull(indexResponse);

    final CountDownLatch latch = new CountDownLatch(1);
    client.index(indexRequest, new ActionListener<IndexResponse>() {
      @Override
      public void onResponse(IndexResponse indexResponse) {
        latch.countDown();
      }

      @Override
      public void onFailure(Exception e) {
        latch.countDown();
      }
    });

    latch.await(30, TimeUnit.SECONDS);
    client.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(2, finishedSpans.size());
    checkSpans(finishedSpans, "IndexRequest");
    assertNull(mockTracer.activeSpan());
  }

  private void checkSpans(List<MockSpan> mockSpans, String expectedOperationName) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
      assertEquals(SpanDecorator.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
      assertEquals(0, mockSpan.parentId());
      String operationName = mockSpan.operationName();
      assertTrue(operationName.equals(expectedOperationName));
    }
  }

  private static class PluginConfigurableNode extends Node {

    public PluginConfigurableNode(Settings settings,
        Collection<Class<? extends Plugin>> classpathPlugins) {
      super(InternalSettingsPreparer.prepareEnvironment(settings, null), classpathPlugins);
    }
  }
}