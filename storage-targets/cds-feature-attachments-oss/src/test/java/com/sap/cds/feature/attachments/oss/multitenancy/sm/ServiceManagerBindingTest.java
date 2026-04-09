/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceManagerBindingTest {

  @Test
  void testGetCredentialsReturnsProvidedMap() {
    Map<String, Object> creds =
        Map.of("host", "s3.amazonaws.com", "bucket", "my-bucket", "region", "eu-west-1");
    var binding = new ServiceManagerBinding(creds);

    assertThat(binding.getCredentials()).isEqualTo(creds);
  }

  @Test
  void testGetServiceNameReturnsObjectstore() {
    var binding = new ServiceManagerBinding(Map.of());

    assertThat(binding.getServiceName()).hasValue("objectstore");
  }

  @Test
  void testGetKeysReturnsCredentialKeys() {
    var binding = new ServiceManagerBinding(Map.of("host", "example.com", "bucket", "b1"));

    assertThat(binding.getKeys()).containsExactlyInAnyOrder("host", "bucket");
  }

  @Test
  void testContainsKeyReturnsTrueForPresent() {
    var binding = new ServiceManagerBinding(Map.of("host", "example.com"));

    assertThat(binding.containsKey("host")).isTrue();
    assertThat(binding.containsKey("missing")).isFalse();
  }

  @Test
  void testGetReturnsValueForPresentKey() {
    var binding = new ServiceManagerBinding(Map.of("host", "example.com"));

    assertThat(binding.get("host")).hasValue("example.com");
    assertThat(binding.get("missing")).isEmpty();
  }

  @Test
  void testGetNameReturnsEmpty() {
    var binding = new ServiceManagerBinding(Map.of());

    assertThat(binding.getName()).isEmpty();
  }

  @Test
  void testGetServicePlanReturnsEmpty() {
    var binding = new ServiceManagerBinding(Map.of());

    assertThat(binding.getServicePlan()).isEmpty();
  }

  @Test
  void testGetTagsReturnsEmptyList() {
    var binding = new ServiceManagerBinding(Map.of());

    assertThat(binding.getTags()).isEmpty();
  }

  @Test
  void testCredentialsAreUnmodifiable() {
    var binding = new ServiceManagerBinding(Map.of("key", "value"));

    org.junit.jupiter.api.Assertions.assertThrows(
        UnsupportedOperationException.class, () -> binding.getCredentials().put("new", "val"));
  }
}
