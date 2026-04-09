/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adapts a Service Manager binding response (JSON credentials map) into the {@link ServiceBinding}
 * interface expected by {@link com.sap.cds.feature.attachments.oss.client.OSClientFactory#create}.
 */
public class ServiceManagerBinding implements ServiceBinding {

  private final Map<String, Object> credentials;

  /**
   * Creates a new adapter from the credentials map returned by the Service Manager binding API.
   *
   * @param credentials the credentials from the SM binding response
   */
  public ServiceManagerBinding(Map<String, Object> credentials) {
    this.credentials = Collections.unmodifiableMap(credentials);
  }

  @Override
  public Map<String, Object> getCredentials() {
    return credentials;
  }

  @Override
  public Set<String> getKeys() {
    return credentials.keySet();
  }

  @Override
  public boolean containsKey(String key) {
    return credentials.containsKey(key);
  }

  @Override
  public Optional<Object> get(String key) {
    return Optional.ofNullable(credentials.get(key));
  }

  @Override
  public Optional<String> getName() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getServiceName() {
    return Optional.of("objectstore");
  }

  @Override
  public Optional<String> getServicePlan() {
    return Optional.empty();
  }

  @Override
  public List<String> getTags() {
    return Collections.emptyList();
  }
}
