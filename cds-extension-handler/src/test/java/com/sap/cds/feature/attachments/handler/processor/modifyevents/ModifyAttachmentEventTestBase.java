package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentModificationResult;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;

abstract class ModifyAttachmentEventTestBase {

		protected ModifyAttachmentEvent cut;
		protected AttachmentService attachmentService;
		protected Path path;
		protected ResolvedSegment target;

		void setup() {
				attachmentService = mock(AttachmentService.class);
				cut = defineCut();

				path = mock(Path.class);
				target = mock(ResolvedSegment.class);
				when(path.target()).thenReturn(target);
		}

		abstract ModifyAttachmentEvent defineCut();

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void contentIsReturnedIfNotExternalStored(boolean isExternalStored) throws AttachmentAccessException, IOException {
				var fieldNames = getDefaultFieldNames();
				var attachment = com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Attachment.create();

				var testContent = "test content";
				try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
						attachment.setContent(testContentStream);
						attachment.setId(UUID.randomUUID().toString());
				}
				when(target.values()).thenReturn(attachment);
				when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(isExternalStored, "id"));
				when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(isExternalStored, "id"));

				var result = cut.processEvent(path, null, fieldNames, attachment.getContent(), CdsData.create(), attachment.getId());

				var expectedContent = isExternalStored ? null : attachment.getContent();
				assertThat(result).isEqualTo(expectedContent);
		}

		AttachmentFieldNames getDefaultFieldNames() {
				return new AttachmentFieldNames("key", Optional.of("documentId"), Optional.of("mimeType"), Optional.of("filename"));
		}

}
