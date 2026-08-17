/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

/**
 * Guard invoked by {@link LazyProxyInputStream} right before attachment content bytes are served.
 *
 * <p>It enforces the malware scan status and freshness policy (rescan-on-download) for the content
 * that is about to be read, regardless of the read shape (content-only {@code $value} reads as well
 * as keyed reads that stream the content). Implementations throw an {@link
 * AttachmentStatusException} to block the download when the content is not clean or must be
 * rescanned first.
 */
@FunctionalInterface
public interface ContentReadGuard {

  /**
   * Enforces the status and freshness policy before content is served.
   *
   * @throws AttachmentStatusException if the content must not be served (e.g. not clean or a rescan
   *     has been triggered)
   */
  void verify();
}
