/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import java.time.Duration;
import java.time.Instant;

/** A cached {@link OSClient} with creation timestamp for TTL-based expiry. */
record CachedOSClient(OSClient client, Instant createdAt) {

  boolean isExpired(Duration ttl) {
    return Duration.between(createdAt, Instant.now()).compareTo(ttl) > 0;
  }
}
