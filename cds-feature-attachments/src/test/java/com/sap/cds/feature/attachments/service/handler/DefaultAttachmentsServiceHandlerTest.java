package com.sap.cds.feature.attachments.service.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;

class DefaultAttachmentsServiceHandlerTest {

	private DefaultAttachmentsServiceHandler cut;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentsServiceHandler();
	}

	@Test
	void createAttachmentsSetData() {
		var createContext = AttachmentCreateEventContext.create();
		var attachmentId = "test ID";
		createContext.setAttachmentIds(Map.of(Attachments.ID, attachmentId, "OtherId", "OtherID value"));

		cut.createAttachment(createContext);

		assertThat(createContext.isCompleted()).isTrue();
		assertThat(createContext.getDocumentId()).isEqualTo(attachmentId);
		assertThat(createContext.getIsExternalCreated()).isFalse();
	}

	@Test
	void updateAttachmentsSetData() {
		var updateContext = AttachmentUpdateEventContext.create();
		var documentId = "test document ID";
		updateContext.setAttachmentIds(Map.of(Attachments.ID, "test ID", "OtherId", "OtherID value"));
		updateContext.setDocumentId(documentId);

		cut.updateAttachment(updateContext);

		assertThat(updateContext.isCompleted()).isTrue();
		assertThat(updateContext.getDocumentId()).isEqualTo(documentId);
		assertThat(updateContext.getIsExternalCreated()).isFalse();
	}

	@Test
	void deleteAttachmentSetData() {
		var deleteContext = AttachmentDeleteEventContext.create();

		cut.deleteAttachment(deleteContext);

		assertThat(deleteContext.isCompleted()).isTrue();
	}

	@Test
	void readAttachmentSetData() {
		var readContext = AttachmentReadEventContext.create();

		cut.readAttachment(readContext);

		assertThat(readContext.isCompleted()).isTrue();
	}

}
