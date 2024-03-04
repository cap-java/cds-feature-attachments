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
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;

abstract class ModifyAttachmentEventTestBase {

	protected static final String TEST_FULL_NAME = "test.full.Name";
	protected ModifyAttachmentEvent cut;
	protected AttachmentService attachmentService;
	protected Path path;
	protected ResolvedSegment target;
	protected CdsEntity entity;

	void setup() {
		attachmentService = mock(AttachmentService.class);
		cut = defineCut();

		path = mock(Path.class);
		target = mock(ResolvedSegment.class);
		entity = mock(CdsEntity.class);
		when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		when(target.entity()).thenReturn(entity);
		when(path.target()).thenReturn(target);
	}

	abstract ModifyAttachmentEvent defineCut();

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void contentIsReturnedIfNotExternalStored(boolean isExternalStored) throws IOException {
		var fieldNames = getDefaultFieldNames();
		var attachment = Attachments.create();

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
		return new AttachmentFieldNames("key", Optional.of(Attachments.DOCUMENT_ID), Optional.of(MediaData.MIME_TYPE), Optional.of(MediaData.FILE_NAME), MediaData.CONTENT);
	}

}
