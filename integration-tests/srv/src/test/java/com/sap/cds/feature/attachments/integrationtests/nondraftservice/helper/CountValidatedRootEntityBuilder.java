/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.CountValidatedRoots;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Builder for creating {@link CountValidatedRoots} entities for count validation tests. This
 * entity has @Validation.MinItems: 1 and @Validation.MaxItems: 3 annotations.
 */
public class CountValidatedRootEntityBuilder {

  private final CountValidatedRoots rootEntity;

  private CountValidatedRootEntityBuilder() {
    rootEntity = CountValidatedRoots.create();
  }

  public static CountValidatedRootEntityBuilder create() {
    return new CountValidatedRootEntityBuilder();
  }

  public CountValidatedRootEntityBuilder setTitle(String title) {
    rootEntity.setTitle(title);
    return this;
  }

  public CountValidatedRootEntityBuilder addCountValidatedAttachments(
      AttachmentsBuilder... attachments) {
    if (rootEntity.getCountValidatedAttachments() == null) {
      rootEntity.setCountValidatedAttachments(new ArrayList<>());
    }
    Arrays.stream(attachments)
        .forEach(attachment -> rootEntity.getCountValidatedAttachments().add(attachment.build()));
    return this;
  }

  public CountValidatedRoots build() {
    return rootEntity;
  }
}
