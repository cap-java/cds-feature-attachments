/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RootEntityBuilder {

  private final Roots rootEntity;

  private RootEntityBuilder() {
    rootEntity = Roots.create();
    rootEntity.setAttachments(new ArrayList<>());
    rootEntity.setItems(new ArrayList<>());
  }

  public static RootEntityBuilder create() {
    return new RootEntityBuilder();
  }

  public RootEntityBuilder setTitle(String title) {
    rootEntity.setTitle(title);
    return this;
  }

  public RootEntityBuilder addAttachments(AttachmentsEntityBuilder... attachments) {
    Arrays.stream(attachments)
        .forEach(attachment -> rootEntity.getAttachments().add(attachment.build()));
    return this;
  }

  public RootEntityBuilder addSizeLimitedAttachments(AttachmentsBuilder... attachments) {
    if (rootEntity.getSizeLimitedAttachments() == null) {
      rootEntity.setSizeLimitedAttachments(new ArrayList<>());
    }
    Arrays.stream(attachments)
        .forEach(attachment -> rootEntity.getSizeLimitedAttachments().add(attachment.build()));
    return this;
  }

  public RootEntityBuilder addItems(ItemEntityBuilder... items) {
    Arrays.stream(items).forEach(item -> rootEntity.getItems().add(item.build()));
    return this;
  }

  @SuppressWarnings("unchecked")
  public RootEntityBuilder addMaxLimitedAttachments(int count) {
    List<CdsData> list = (List<CdsData>) rootEntity.get("maxLimitedAttachments");
    if (list == null) {
      list = new ArrayList<>();
      rootEntity.put("maxLimitedAttachments", list);
    }
    for (int i = 0; i < count; i++) {
      CdsData item = CdsData.create();
      item.put("name", "item" + i);
      list.add(item);
    }
    return this;
  }

  public Roots build() {
    return rootEntity;
  }
}
