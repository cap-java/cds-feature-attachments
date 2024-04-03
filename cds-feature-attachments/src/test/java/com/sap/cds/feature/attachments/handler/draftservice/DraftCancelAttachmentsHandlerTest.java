package com.sap.cds.feature.attachments.handler.draftservice;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifierProvider;

class DraftCancelAttachmentsHandlerTest {

	private DraftCancelAttachmentsHandler cut;
	private AttachmentsReader attachmentsReader;
	private ModifyAttachmentEvent deleteContentAttachmentEvent;
	private ActiveEntityModifierProvider modifierProvider;

	@BeforeEach
	void setup() {
		attachmentsReader = mock(AttachmentsReader.class);
		deleteContentAttachmentEvent = mock(ModifyAttachmentEvent.class);
		modifierProvider = mock(ActiveEntityModifierProvider.class);
		cut = new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentAttachmentEvent, modifierProvider);
	}

	@Test
	void testProcessBeforeDraftCancel() {
		fail("logic and test not implemented yet");
	}

}