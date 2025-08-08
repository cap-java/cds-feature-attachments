/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;

public class AttachmentsEntityBuilder {

	private AttachmentEntity attachmentEntity = AttachmentEntity.create();

	private AttachmentsEntityBuilder() {
	}

	public static AttachmentsEntityBuilder create() {
		return new AttachmentsEntityBuilder();
	}

	public AttachmentsEntityBuilder setMimeType(String mimeType) {
		attachmentEntity.setMimeType(mimeType);
		return this;
	}

	public AttachmentsEntityBuilder setFileName(String fileName) {
		attachmentEntity.setFileName(fileName);
		return this;
	}

	public AttachmentEntity build() {
		return attachmentEntity;
	}

}
