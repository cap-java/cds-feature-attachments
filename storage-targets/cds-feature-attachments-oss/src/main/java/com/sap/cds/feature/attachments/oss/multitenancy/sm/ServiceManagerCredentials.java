/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Map;

/**
 * Holds the credentials required to authenticate with SAP BTP Service Manager. Typically extracted
 * from a {@code service-manager} service binding.
 */
public record ServiceManagerCredentials(
    String smUrl,
    String authUrl,
    String clientId,
    String clientSecret,
    String certificate,
    String key,
    String certUrl) {

  /**
   * Creates {@link ServiceManagerCredentials} from a {@code service-manager} {@link
   * ServiceBinding}.
   *
   * @param binding the service-manager service binding
   * @return the extracted credentials
   */
  public static ServiceManagerCredentials fromServiceBinding(ServiceBinding binding) {
    Map<String, Object> creds = binding.getCredentials();
    return new ServiceManagerCredentials(
        (String) creds.get("sm_url"),
        (String) creds.get("url"),
        (String) creds.get("clientid"),
        (String) creds.get("clientsecret"),
        (String) creds.get("certificate"),
        (String) creds.get("key"),
        (String) creds.get("certurl"));
  }

  /** Returns {@code true} if mTLS credentials are available. */
  public boolean hasMtlsCredentials() {
    return certificate != null && !certificate.isEmpty() && key != null && !key.isEmpty();
  }
}
