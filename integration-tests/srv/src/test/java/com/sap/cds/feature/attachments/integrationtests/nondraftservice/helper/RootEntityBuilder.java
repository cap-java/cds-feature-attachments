/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import java.util.ArrayList;
import java.util.Arrays;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;

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
		Arrays.stream(attachments).forEach(attachment -> rootEntity.getAttachments().add(attachment.build()));
		return this;
	}

	public RootEntityBuilder addItems(ItemEntityBuilder... items) {
		Arrays.stream(items).forEach(item -> rootEntity.getItems().add(item.build()));
		return this;
	}

	public Roots build() {
		return rootEntity;
	}


}
