/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * Runs the multitenancy OSS storage integration tests against a real Azure Blob Storage instance.
 * Skipped automatically if no objectstore binding with Azure credentials is available in
 * VCAP_SERVICES.
 */
class AzureMtxOssStorageTest extends AbstractMtxOssStorageTest {

  @Override
  protected ServiceBinding getServiceBinding() {
    return DefaultServiceBindingAccessor.getInstance().getServiceBindings().stream()
        .filter(b -> b.getServiceName().map("objectstore"::equals).orElse(false))
        .filter(b -> b.getCredentials().containsKey("container_uri"))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected String getProviderName() {
    return "Azure Blob Storage";
  }
}
