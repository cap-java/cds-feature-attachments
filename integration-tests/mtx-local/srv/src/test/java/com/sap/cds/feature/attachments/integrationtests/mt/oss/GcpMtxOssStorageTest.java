/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * Runs the multitenancy OSS storage integration tests against a real Google Cloud Storage instance.
 * Skipped automatically if no objectstore binding with GCP credentials is available in
 * VCAP_SERVICES.
 */
class GcpMtxOssStorageTest extends AbstractMtxOssStorageTest {

  @Override
  protected ServiceBinding getServiceBinding() {
    return DefaultServiceBindingAccessor.getInstance().getServiceBindings().stream()
        .filter(b -> b.getServiceName().map("objectstore"::equals).orElse(false))
        .filter(b -> b.getCredentials().containsKey("base64EncodedPrivateKeyData"))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected String getProviderName() {
    return "Google Cloud Storage";
  }
}
