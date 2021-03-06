package me.j360.trace.okhttp.test;

import me.j360.trace.collector.core.ClientRequestInterceptor;
import me.j360.trace.collector.core.ClientResponseInterceptor;
import me.j360.trace.collector.core.ClientTracer;
import me.j360.trace.collector.core.SpanId;
import me.j360.trace.core.TraceKeys;
import me.j360.trace.http.BraveHttpHeaders;
import me.j360.trace.http.DefaultSpanNameProvider;
import me.j360.trace.okhttp.BraveOkHttpRequestResponseInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BraveOkHttpRequestResponseInterceptorTest {

  private static final Long SPAN_ID = 151864L;
  private static final Long TRACE_ID = 8494864L;
  private static final String HTTP_METHOD_GET = "GET";

  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  @Rule
  public final MockWebServer server = new MockWebServer();

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private ClientTracer clientTracer;

  private SpanId spanId;
  private OkHttpClient client;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    this.spanId = SpanId.builder().spanId(SPAN_ID).traceId(TRACE_ID).parentId(null).build();
    this.client = new OkHttpClient.Builder()
            .addInterceptor(new BraveOkHttpRequestResponseInterceptor(new ClientRequestInterceptor(clientTracer), new ClientResponseInterceptor(clientTracer), new DefaultSpanNameProvider()))
            .build();
  }

  @Test
  public void testTracingTrue() throws IOException, InterruptedException {
    when(clientTracer.startNewSpan(HTTP_METHOD_GET)).thenReturn(spanId);

    String url = "http://localhost:" + server.getPort() + "/foo";
    Request request = new Request.Builder()
            .url(url)
            .build();

    server.enqueue(new MockResponse()
            .setBody("bar")
            .setResponseCode(200)
    );

    Response response = client.newCall(request).execute();

    assertEquals(200, response.code());

    InOrder inOrder = inOrder(clientTracer);
    inOrder.verify(clientTracer).startNewSpan(HTTP_METHOD_GET);
    inOrder.verify(clientTracer).submitBinaryAnnotation(TraceKeys.HTTP_URL, url);
    inOrder.verify(clientTracer).setClientSent();
    inOrder.verify(clientTracer).setClientReceived();
    verifyNoMoreInteractions(clientTracer);

    RecordedRequest serverRequest = server.takeRequest();
    assertEquals(HTTP_METHOD_GET, serverRequest.getMethod());
    assertEquals("1", serverRequest.getHeader(BraveHttpHeaders.Sampled.getName()));
    assertEquals(Long.toString(TRACE_ID, 16), serverRequest.getHeader(BraveHttpHeaders.TraceId.getName()));
    assertEquals(Long.toString(SPAN_ID, 16), serverRequest.getHeader(BraveHttpHeaders.SpanId.getName()));
  }

  @Test
  public void testTracingTrueHttpNoOk() throws IOException, InterruptedException {
    when(clientTracer.startNewSpan(HTTP_METHOD_GET)).thenReturn(spanId);

    String url = "http://localhost:" + server.getPort() + "/foo";
    Request request = new Request.Builder()
            .url(url)
            .build();

    server.enqueue(new MockResponse()
            .setBody("bar")
            .setResponseCode(400)
    );

    Response response = client.newCall(request).execute();

    assertEquals(400, response.code());

    InOrder inOrder = inOrder(clientTracer);
    inOrder.verify(clientTracer).startNewSpan(HTTP_METHOD_GET);
    inOrder.verify(clientTracer).submitBinaryAnnotation(TraceKeys.HTTP_URL, url);
    inOrder.verify(clientTracer).setClientSent();
    inOrder.verify(clientTracer).submitBinaryAnnotation(TraceKeys.HTTP_STATUS_CODE, "400");
    inOrder.verify(clientTracer).setClientReceived();
    verifyNoMoreInteractions(clientTracer);

    RecordedRequest serverRequest = server.takeRequest();
    assertEquals(HTTP_METHOD_GET, serverRequest.getMethod());
    assertEquals("1", serverRequest.getHeader(BraveHttpHeaders.Sampled.getName()));
    assertEquals(Long.toString(TRACE_ID, 16), serverRequest.getHeader(BraveHttpHeaders.TraceId.getName()));
    assertEquals(Long.toString(SPAN_ID, 16), serverRequest.getHeader(BraveHttpHeaders.SpanId.getName()));
  }

  @Test
  public void testTracingFalse() throws IOException, InterruptedException {
    when(clientTracer.startNewSpan(HTTP_METHOD_GET)).thenReturn(null);

    String url = "http://localhost:" + server.getPort() + "/foo";
    Request request = new Request.Builder()
            .url(url)
            .build();

    server.enqueue(new MockResponse()
            .setBody("bar")
    );

    Response response = client.newCall(request).execute();

    assertEquals(200, response.code());

    InOrder inOrder = inOrder(clientTracer);
    inOrder.verify(clientTracer).startNewSpan(HTTP_METHOD_GET);
    inOrder.verify(clientTracer).setClientReceived();
    verifyNoMoreInteractions(clientTracer);

    RecordedRequest serverRequest = server.takeRequest();
    assertEquals(HTTP_METHOD_GET, serverRequest.getMethod());
    assertEquals("0", serverRequest.getHeader(BraveHttpHeaders.Sampled.getName()));
  }

  @Test
  public void testQueryParams() throws IOException, InterruptedException {
    when(clientTracer.startNewSpan(HTTP_METHOD_GET)).thenReturn(spanId);

    String url = "http://localhost:" + server.getPort() + "/foo?z=2&yAA";
    Request request = new Request.Builder()
            .url(url)
            .build();

    server.enqueue(new MockResponse()
            .setBody("bar")
    );

    Response response = client.newCall(request).execute();

    assertEquals(200, response.code());

    InOrder inOrder = inOrder(clientTracer);
    inOrder.verify(clientTracer).startNewSpan(HTTP_METHOD_GET);
    inOrder.verify(clientTracer).submitBinaryAnnotation(TraceKeys.HTTP_URL, url);
    inOrder.verify(clientTracer).setClientSent();
    inOrder.verify(clientTracer).setClientReceived();
    verifyNoMoreInteractions(clientTracer);

    RecordedRequest serverRequest = server.takeRequest();
    assertEquals(HTTP_METHOD_GET, serverRequest.getMethod());
    assertEquals("1", serverRequest.getHeader(BraveHttpHeaders.Sampled.getName()));
    assertEquals(Long.toString(TRACE_ID, 16), serverRequest.getHeader(BraveHttpHeaders.TraceId.getName()));
    assertEquals(Long.toString(SPAN_ID, 16), serverRequest.getHeader(BraveHttpHeaders.SpanId.getName()));
  }

}