package com.sap.cds.feature.attachments.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttachmentAccessExceptionTest {

		private AttachmentAccessException cut;

		@Test
		void exceptionWorks() {
				cut = new AttachmentAccessException();
				assertThrows(AttachmentAccessException.class, () -> {
						throw cut;
				});
		}

}
