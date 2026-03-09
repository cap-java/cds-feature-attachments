/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.service.model;

/**
 * Status codes for the malware scan status of attachments. These constants correspond to the {@code
 * sap.attachments.ScanStates} code list values defined in the CDS model.
 */
public final class StatusCode {
  public static final String UNSCANNED = "Unscanned";
  public static final String SCANNING = "Scanning";
  public static final String CLEAN = "Clean";
  public static final String INFECTED = "Infected";
  public static final String FAILED = "Failed";

  private StatusCode() {}
}
