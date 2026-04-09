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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceManagerTokenProviderTest {

  private HttpClient httpClient;
  private ServiceManagerTokenProvider tokenProvider;

  private static final ServiceManagerCredentials CREDS =
      new ServiceManagerCredentials(
          "https://sm.example.com",
          "https://auth.example.com",
          "my-client",
          "my-secret",
          null,
          null,
          null);

  @SuppressWarnings("unchecked")
  private static HttpResponse<String> mockResponse(int status, String body) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    return response;
  }

  @BeforeEach
  void setup() {
    httpClient = mock(HttpClient.class);
    tokenProvider = new ServiceManagerTokenProvider(CREDS, httpClient);
  }

  @Test
  void testGetAccessTokenFetchesFromAuthUrl() throws Exception {
    doReturn(mockResponse(200, "{\"access_token\":\"tok123\",\"expires_in\":3600}"))
        .when(httpClient)
        .send(any(), any());

    String token = tokenProvider.getAccessToken();

    assertThat(token).isEqualTo("tok123");
  }

  @Test
  void testGetAccessTokenCachesToken() throws Exception {
    doReturn(mockResponse(200, "{\"access_token\":\"tok123\",\"expires_in\":3600}"))
        .when(httpClient)
        .send(any(), any());

    tokenProvider.getAccessToken();
    tokenProvider.getAccessToken();

    verify(httpClient, times(1)).send(any(), any());
  }

  @Test
  void testGetAccessTokenThrowsOnHttpError() throws Exception {
    doReturn(mockResponse(401, "Unauthorized")).when(httpClient).send(any(), any());

    assertThrows(ServiceManagerException.class, () -> tokenProvider.getAccessToken());
  }

  @Test
  void testGetAccessTokenThrowsOnIoException() throws Exception {
    doThrow(new IOException("Connection refused")).when(httpClient).send(any(), any());

    assertThrows(ServiceManagerException.class, () -> tokenProvider.getAccessToken());
  }

  @Test
  void testGetAccessTokenThrowsOnInterruptedException() throws Exception {
    doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(), any());

    assertThrows(ServiceManagerException.class, () -> tokenProvider.getAccessToken());
    Thread.interrupted();
  }
}
