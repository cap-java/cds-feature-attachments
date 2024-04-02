package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.EventContextHolder;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;

@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class DraftOdataRequestValidationWithTestHandlerTest extends DraftOdataRequestValidationBase {

	@Test
	void serviceHandlerIsNotEmpty() {
		assertThat(serviceHandler).isNotNull();
		verifyNoAttachmentEventsCalled();
	}

	@Override
	protected void verifyDocumentId(String documentId, String attachmentId) {
		assertThat(documentId).isNotEmpty().isNotEqualTo(attachmentId);
	}

	@Override
	protected void verifyContent(InputStream attachment, String testContent) {
		assertThat(attachment).isNull();
	}

	@Override
	protected void verifyNoAttachmentEventsCalled() {
		assertThat(serviceHandler.getEventContext()).isEmpty();
	}

	@Override
	protected void clearServiceHandlerContext() {
		serviceHandler.clearEventContext();
	}

	@Override
	protected void verifyEventContextEmptyForEvent(String... events) {
		Arrays.stream(events).forEach(event -> {
			assertThat(serviceHandler.getEventContextForEvent(event)).isEmpty();
		});
	}

	@Override
	protected void verifyOnlyTwoCreateEvents(String newAttachmentContent, String newAttachmentEntityContent) {
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_MARK_AS_DELETED, AttachmentService.EVENT_READ_ATTACHMENT);
		var createEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
		assertThat(createEvents).hasSize(2);
		var attachmentContentFound = isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentContent);
		assertThat(attachmentContentFound).isTrue();
		var attachmentEntityContentFound = isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentEntityContent);
		assertThat(attachmentEntityContentFound).isTrue();
	}

	@Override
	protected void verifyTwoCreateAndDeleteEvents(String newAttachmentContent, String newAttachmentEntityContent) {
		awaitNumberOfExpectedEvents(4);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
		var createEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
		assertThat(createEvents).hasSize(2);
		var attachmentContentFound = isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentContent);
		assertThat(attachmentContentFound).isTrue();
		var attachmentEntityContentFound = isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentEntityContent);
		assertThat(attachmentEntityContentFound).isTrue();
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_AS_DELETED);
		assertThat(deleteEvents).hasSize(2);
		deleteEvents.forEach(event -> {
			var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
			assertThat(deleteContext.getDocumentId()).isNotEmpty();
			var createEventFound = createEvents.stream().anyMatch(createEvent -> {
				var createContext = (AttachmentCreateEventContext) createEvent.context();
				return createContext.getDocumentId().equals(deleteContext.getDocumentId());
			});
			assertThat(createEventFound).isTrue();
		});
	}

	@Override
	protected void verifyTwoReadEvents() {
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_MARK_AS_DELETED, AttachmentService.EVENT_CREATE_ATTACHMENT);
		var readEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
		assertThat(readEvents).hasSize(2);
	}

	@Override
	protected void verifyOnlyTwoDeleteEvents(String attachmentDocumentId, String attachmentEntityDocumentId) {
		awaitNumberOfExpectedEvents(2);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_AS_DELETED);
		assertThat(deleteEvents).hasSize(2);
		verifyDeleteEventContainsDocumentId(deleteEvents, attachmentDocumentId);
		verifyDeleteEventContainsDocumentId(deleteEvents, attachmentEntityDocumentId);
	}

	@Override
	protected void verifyTwoUpdateEvents(String newAttachmentContent, String attachmentDocumentId, String newAttachmentEntityContent, String attachmentEntityDocumentId) {
		awaitNumberOfExpectedEvents(4);
		var createEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_AS_DELETED);
		assertThat(createEvents).hasSize(2);
		verifyCreateEventFound(createEvents, newAttachmentContent);
		verifyCreateEventFound(createEvents, newAttachmentEntityContent);
		assertThat(deleteEvents).hasSize(2);
		verifyDeleteEventContainsDocumentId(deleteEvents, attachmentDocumentId);
		verifyDeleteEventContainsDocumentId(deleteEvents, attachmentEntityDocumentId);
	}

	private void awaitNumberOfExpectedEvents(int expectedEvents) {
		Awaitility.await().until(() -> serviceHandler.getEventContext().size() == expectedEvents);
	}

	private void verifyCreateEventFound(List<EventContextHolder> createEvents, String newContent) {
		var eventContentFound = createEvents.stream().anyMatch(event -> {
			var createContext = (AttachmentCreateEventContext) event.context();
			try {
				return Arrays.equals(createContext.getData().getContent()
																											.readAllBytes(), newContent.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		assertThat(eventContentFound).isTrue();
	}

	private boolean isAttachmentContentFoundInCreateEvent(List<EventContextHolder> createEvents, String newAttachmentContent) {
		return createEvents.stream().anyMatch(event -> {
			var createContext = (AttachmentCreateEventContext) event.context();
			try {
				return Arrays.equals(createContext.getData().getContent()
																											.readAllBytes(), newAttachmentContent.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void verifyDeleteEventContainsDocumentId(List<EventContextHolder> deleteEvents, String documentId) {
		var eventFound = deleteEvents.stream().anyMatch(event -> {
			var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
			return deleteContext.getDocumentId().equals(documentId);
		});
		assertThat(eventFound).isTrue();
	}

}
