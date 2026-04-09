/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.services.ServiceException;

/** Thrown when a request is made for a tenant that has no provisioned object store. */
public class TenantNotProvisionedException extends ServiceException {

  public TenantNotProvisionedException(String tenantId) {
    super("No object store provisioned for tenant: " + tenantId);
  }
}
