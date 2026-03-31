/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items;
import java.util.ArrayList;
import java.util.Arrays;

public class ItemEntityBuilder {

  private final Items item;

  private ItemEntityBuilder() {
    item = Items.create();
    item.setAttachments(new ArrayList<>());
    item.setAttachmentEntities(new ArrayList<>());
  }

  public static ItemEntityBuilder create() {
    return new ItemEntityBuilder();
  }

  public ItemEntityBuilder setTitle(String title) {
    item.setTitle(title);
    return this;
  }

  public ItemEntityBuilder addAttachmentEntities(AttachmentsEntityBuilder... attachmentEntities) {
    Arrays.stream(attachmentEntities)
        .forEach(attachment -> item.getAttachmentEntities().add(attachment.build()));
    return this;
  }

  public ItemEntityBuilder addAttachments(AttachmentsBuilder... attachmentEntities) {
    Arrays.stream(attachmentEntities)
        .forEach(attachment -> item.getAttachments().add(attachment.build()));
    return this;
  }

  public Items build() {
    return item;
  }
}
