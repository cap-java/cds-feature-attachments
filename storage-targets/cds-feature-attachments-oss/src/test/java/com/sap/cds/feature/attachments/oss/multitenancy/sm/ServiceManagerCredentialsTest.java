/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceManagerCredentialsTest {

  @Test
  void testFromServiceBindingExtractsAllFields() {
    ServiceBinding binding = mock(ServiceBinding.class);
    Map<String, Object> creds = new HashMap<>();
    creds.put("sm_url", "https://sm.example.com");
    creds.put("url", "https://auth.example.com");
    creds.put("clientid", "my-client");
    creds.put("clientsecret", "my-secret");
    creds.put("certificate", "cert-pem");
    creds.put("key", "key-pem");
    creds.put("certurl", "https://cert-auth.example.com");
    when(binding.getCredentials()).thenReturn(creds);

    ServiceManagerCredentials result = ServiceManagerCredentials.fromServiceBinding(binding);

    assertThat(result.smUrl()).isEqualTo("https://sm.example.com");
    assertThat(result.authUrl()).isEqualTo("https://auth.example.com");
    assertThat(result.clientId()).isEqualTo("my-client");
    assertThat(result.clientSecret()).isEqualTo("my-secret");
    assertThat(result.certificate()).isEqualTo("cert-pem");
    assertThat(result.key()).isEqualTo("key-pem");
    assertThat(result.certUrl()).isEqualTo("https://cert-auth.example.com");
  }

  @Test
  void testFromServiceBindingHandlesNullOptionalFields() {
    ServiceBinding binding = mock(ServiceBinding.class);
    Map<String, Object> creds = new HashMap<>();
    creds.put("sm_url", "https://sm.example.com");
    creds.put("url", "https://auth.example.com");
    creds.put("clientid", "my-client");
    creds.put("clientsecret", "my-secret");
    when(binding.getCredentials()).thenReturn(creds);

    ServiceManagerCredentials result = ServiceManagerCredentials.fromServiceBinding(binding);

    assertThat(result.certificate()).isNull();
    assertThat(result.key()).isNull();
    assertThat(result.certUrl()).isNull();
  }

  @Test
  void testHasMtlsCredentialsReturnsTrueWhenBothPresent() {
    var creds =
        new ServiceManagerCredentials(
            "url", "auth", "id", "secret", "cert-pem", "key-pem", "certurl");

    assertThat(creds.hasMtlsCredentials()).isTrue();
  }

  @Test
  void testHasMtlsCredentialsReturnsFalseWhenMissing() {
    var creds = new ServiceManagerCredentials("url", "auth", "id", "secret", null, null, null);

    assertThat(creds.hasMtlsCredentials()).isFalse();
  }

  @Test
  void testHasMtlsCredentialsReturnsFalseWhenEmpty() {
    var creds = new ServiceManagerCredentials("url", "auth", "id", "secret", "", "", "certurl");

    assertThat(creds.hasMtlsCredentials()).isFalse();
  }
}
