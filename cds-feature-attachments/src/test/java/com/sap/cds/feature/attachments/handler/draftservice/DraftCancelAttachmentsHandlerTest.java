package com.sap.cds.feature.attachments.handler.draftservice;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;

class DraftCancelAttachmentsHandlerTest {

	private DraftCancelAttachmentsHandler cut;
	private AttachmentsReader attachmentsReader;
	private ModifyAttachmentEvent deleteContentAttachmentEvent;

	@BeforeEach
	void setup() {
		attachmentsReader = mock(AttachmentsReader.class);
		deleteContentAttachmentEvent = mock(ModifyAttachmentEvent.class);
		cut = new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentAttachmentEvent);
	}

	@Test
	void testProcessBeforeDraftCancel() {
		fail("logic and test not implemented yet");
	}

}