/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServiceManagerClientTest {

  private HttpClient httpClient;
  private ServiceManagerTokenProvider tokenProvider;
  private ServiceManagerClient client;

  private static final ServiceManagerCredentials CREDS =
      new ServiceManagerCredentials(
          "https://sm.example.com", "https://auth.example.com", "id", "secret", null, null, null);

  @SuppressWarnings("unchecked")
  private static HttpResponse<String> mockResponse(int status, String body) {
    return mockResponse(status, body, Map.of());
  }

  @SuppressWarnings("unchecked")
  private static HttpResponse<String> mockResponse(
      int status, String body, Map<String, List<String>> headers) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    when(response.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
    return response;
  }

  @BeforeEach
  void setup() {
    httpClient = mock(HttpClient.class);
    tokenProvider = mock(ServiceManagerTokenProvider.class);
    when(tokenProvider.getAccessToken()).thenReturn("test-token");
    // Use short poll timeout for tests
    client = new ServiceManagerClient(CREDS, tokenProvider, httpClient, Duration.ofSeconds(1));
  }

  @Nested
  class ReadOperations {

    @Test
    void testGetOfferingIdReturnsId() throws Exception {
      doReturn(
              mockResponse(
                  200, "{\"items\":[{\"id\":\"offering-123\",\"name\":\"objectstore\"}]}"))
          .when(httpClient)
          .send(any(), any());

      String id = client.getOfferingId();

      assertThat(id).isEqualTo("offering-123");
    }

    @Test
    void testGetOfferingIdThrowsWhenNotFound() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[]}")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.getOfferingId());
    }

    @Test
    void testGetPlanIdReturnsId() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[{\"id\":\"plan-456\"}]}"))
          .when(httpClient)
          .send(any(), any());

      String id = client.getPlanId("offering-123");

      assertThat(id).isEqualTo("plan-456");
    }

    @Test
    void testGetPlanIdThrowsWhenNotFound() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[]}")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.getPlanId("offering-123"));
    }

    @Test
    void testGetBindingReturnsResultWhenFound() throws Exception {
      String responseBody =
          """
          {"items":[{
            "id":"binding-1",
            "service_instance_id":"inst-1",
            "credentials":{"host":"s3.aws.com","bucket":"tenant-bucket"}
          }]}""";
      doReturn(mockResponse(200, responseBody)).when(httpClient).send(any(), any());

      Optional<ServiceManagerClient.ServiceManagerBindingResult> result =
          client.getBinding("tenant-1");

      assertThat(result).isPresent();
      assertThat(result.get().bindingId()).isEqualTo("binding-1");
      assertThat(result.get().instanceId()).isEqualTo("inst-1");
      assertThat(result.get().credentials()).containsEntry("host", "s3.aws.com");
    }

    @Test
    void testGetBindingReturnsEmptyWhenNotFound() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[]}")).when(httpClient).send(any(), any());

      Optional<ServiceManagerClient.ServiceManagerBindingResult> result =
          client.getBinding("tenant-1");

      assertThat(result).isEmpty();
    }

    @Test
    void testGetInstanceByTenantReturnsIdWhenFound() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[{\"id\":\"inst-1\"}]}"))
          .when(httpClient)
          .send(any(), any());

      Optional<String> result = client.getInstanceByTenant("tenant-1");

      assertThat(result).hasValue("inst-1");
    }

    @Test
    void testGetInstanceByTenantReturnsEmptyWhenNotFound() throws Exception {
      doReturn(mockResponse(200, "{\"items\":[]}")).when(httpClient).send(any(), any());

      Optional<String> result = client.getInstanceByTenant("tenant-1");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class WriteOperations {

    @Test
    void testCreateInstanceSyncReturnsId() throws Exception {
      doReturn(mockResponse(201, "{\"id\":\"inst-new\"}")).when(httpClient).send(any(), any());

      String id = client.createInstance("tenant-1", "plan-123");

      assertThat(id).isEqualTo("inst-new");
    }

    @Test
    void testCreateInstanceAsyncPollsAndReturnsId() throws Exception {
      // First call: async 202 with Location header
      HttpResponse<String> asyncResponse =
          mockResponse(
              202, "", Map.of("Location", List.of("/v1/service_instances/inst-new/operations/op1")));
      // Second call (poll): succeeded
      HttpResponse<String> pollResponse =
          mockResponse(200, "{\"id\":\"inst-new\",\"state\":\"succeeded\"}");

      doReturn(asyncResponse).doReturn(pollResponse).when(httpClient).send(any(), any());

      String id = client.createInstance("tenant-1", "plan-123");

      assertThat(id).isEqualTo("inst-new");
    }

    @Test
    void testCreateInstanceThrowsOnError() throws Exception {
      doReturn(mockResponse(400, "Bad Request")).when(httpClient).send(any(), any());

      assertThrows(
          ServiceManagerException.class, () -> client.createInstance("tenant-1", "plan-123"));
    }

    @Test
    void testCreateBindingReturnsResult() throws Exception {
      String body =
          "{\"id\":\"bind-1\",\"service_instance_id\":\"inst-1\",\"credentials\":{\"bucket\":\"b1\"}}";
      doReturn(mockResponse(201, body)).when(httpClient).send(any(), any());

      var result = client.createBinding("tenant-1", "inst-1");

      assertThat(result.bindingId()).isEqualTo("bind-1");
      assertThat(result.credentials()).containsEntry("bucket", "b1");
    }

    @Test
    void testCreateBindingThrowsOnError() throws Exception {
      doReturn(mockResponse(400, "Bad Request")).when(httpClient).send(any(), any());

      assertThrows(
          ServiceManagerException.class, () -> client.createBinding("tenant-1", "inst-1"));
    }

    @Test
    void testDeleteBindingSucceeds() throws Exception {
      doReturn(mockResponse(200, "")).when(httpClient).send(any(), any());

      client.deleteBinding("bind-1"); // should not throw
    }

    @Test
    void testDeleteBindingThrowsOnError() throws Exception {
      doReturn(mockResponse(500, "Internal Server Error")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.deleteBinding("bind-1"));
    }

    @Test
    void testDeleteInstanceSyncSucceeds() throws Exception {
      doReturn(mockResponse(200, "")).when(httpClient).send(any(), any());

      client.deleteInstance("inst-1"); // should not throw
    }

    @Test
    void testDeleteInstanceAsyncSucceeds() throws Exception {
      HttpResponse<String> asyncResponse =
          mockResponse(202, "", Map.of("Location", List.of("/v1/service_instances/inst-1/op")));
      HttpResponse<String> pollResponse =
          mockResponse(200, "{\"state\":\"succeeded\"}");
      doReturn(asyncResponse).doReturn(pollResponse).when(httpClient).send(any(), any());

      client.deleteInstance("inst-1"); // should not throw
    }

    @Test
    void testDeleteInstanceThrowsOnError() throws Exception {
      doReturn(mockResponse(500, "fail")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.deleteInstance("inst-1"));
    }
  }

  @Nested
  class Polling {

    @Test
    void testPollTimesOut() throws Exception {
      // Always return "in progress" — should timeout
      doReturn(mockResponse(200, "{\"state\":\"in progress\"}"))
          .when(httpClient)
          .send(any(), any());

      assertThrows(
          ServiceManagerException.class,
          () -> client.pollUntilDone("https://sm.example.com/op/1"));
    }

    @Test
    void testPollThrowsOnFailedState() throws Exception {
      doReturn(
              mockResponse(
                  200,
                  "{\"state\":\"failed\",\"last_operation\":{\"state\":\"failed\",\"description\":\"quota exceeded\"}}"))
          .when(httpClient)
          .send(any(), any());

      var ex =
          assertThrows(
              ServiceManagerException.class,
              () -> client.pollUntilDone("https://sm.example.com/op/1"));
      assertThat(ex.getMessage()).contains("quota exceeded");
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void testHttpIoExceptionWrapped() throws Exception {
      doThrow(new IOException("connection reset")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.getOfferingId());
    }

    @Test
    void testHttpInterruptedExceptionWrapped() throws Exception {
      doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(), any());

      assertThrows(ServiceManagerException.class, () -> client.getOfferingId());
      Thread.interrupted();
    }
  }
}
