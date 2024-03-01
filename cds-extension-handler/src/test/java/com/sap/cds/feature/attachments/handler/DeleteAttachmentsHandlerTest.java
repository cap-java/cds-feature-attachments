package com.sap.cds.feature.attachments.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.services.cds.CdsDeleteEventContext;

class DeleteAttachmentsHandlerTest {

		private DeleteAttachmentsHandler cut;

		@BeforeEach
		void setup() {
				cut = new DeleteAttachmentsHandler();
		}

		@Test
		void dummy() {
				//TODO remove if logic is implemented
				cut.deleteAttachments(mock(CdsDeleteEventContext.class));
		}

		@Test
		void checkAnnotations() {
				fail("not implemented");
		}

}
