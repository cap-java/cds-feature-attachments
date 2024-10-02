package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.request.UserInfo;

class MarkAsDeletedAttachmentEventTest {

	private MarkAsDeletedAttachmentEvent cut;
	private AttachmentService attachmentService;
	private Path path;
	private Map<String, Object> currentData;
	private EventContext context;
	private UserInfo userInfo;

	@BeforeEach
	void setup() {
		attachmentService = mock(AttachmentService.class);
		cut = new MarkAsDeletedAttachmentEvent(attachmentService);

		context = mock(EventContext.class);
		path = mock(Path.class);
		var target = mock(ResolvedSegment.class);
		currentData = new HashMap<>();
		when(path.target()).thenReturn(target);
		var eventTarget = mock(CdsEntity.class);
		when(context.getTarget()).thenReturn(eventTarget);
		when(eventTarget.getQualifiedName()).thenReturn("some.qualified.name");
		when(target.values()).thenReturn(currentData);
		userInfo = mock(UserInfo.class);
		when(context.getUserInfo()).thenReturn(userInfo);
	}

	@Test
	void documentIsExternallyDeleted() {
		var value = "test";
		var contentId = "some id";
		var data = Attachments.create();
		data.setContentId(contentId);

		var expectedValue = cut.processEvent(path, value, data, context);

		assertThat(expectedValue).isEqualTo(value);
		assertThat(data.getContentId()).isEqualTo(contentId);
		var deletionInputCaptor = ArgumentCaptor.forClass(MarkAsDeletedInput.class);
		verify(attachmentService).markAttachmentAsDeleted(deletionInputCaptor.capture());
		assertThat(deletionInputCaptor.getValue().contentId()).isEqualTo(contentId);
		assertThat(deletionInputCaptor.getValue().userInfo()).isEqualTo(userInfo);
		assertThat(currentData).containsEntry(Attachments.CONTENT_ID, null);
		assertThat(currentData).containsEntry(Attachments.STATUS, null);
		assertThat(currentData).containsEntry(Attachments.SCANNED_AT, null);
	}

	@Test
	void documentIsNotExternallyDeletedBecauseDoesNotExistBefore() {
		var value = "test";
		var data = Attachments.create();

		var expectedValue = cut.processEvent(path, value, data, context);

		assertThat(expectedValue).isEqualTo(value);
		assertThat(data.getContentId()).isNull();
		verifyNoInteractions(attachmentService);
		assertThat(currentData).containsEntry(Attachments.CONTENT_ID, null);
	}

	@Test
	void documentIsNotExternallyDeletedBecauseItIsDraftChangeEvent() {
		var value = "test";
		var contentId = "some id";
		var data = Attachments.create();
		data.setContentId(contentId);
		when(context.getEvent()).thenReturn(DraftService.EVENT_DRAFT_PATCH);

		var expectedValue = cut.processEvent(path, value, data, context);

		assertThat(expectedValue).isEqualTo(value);
		assertThat(data.getContentId()).isEqualTo(contentId);
		verifyNoInteractions(attachmentService);
		assertThat(currentData).containsEntry(Attachments.CONTENT_ID, null);
	}

}
