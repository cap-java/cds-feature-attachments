package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.EventContextHolder;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;

@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class OdataRequestValidationWithTestHandlerTest extends OdataRequestValidationBase {

	@Test
	void serviceHandlerAvailable() {
		assertThat(serviceHandler).isNotNull();
	}

	@Override
	protected void executeContentRequestAndValidateContent(String url, String content) throws Exception {
		var response = requestHelper.executeGet(url);
		assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
	}

	@Override
	protected void verifyTwoDeleteEvents(AttachmentEntity itemAttachmentEntityAfterChange,
			Attachments itemAttachmentAfterChange) {
		waitTillExpectedHandlerMessageSize(2);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT, AttachmentService.EVENT_CREATE_ATTACHMENT);
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
		assertThat(deleteEvents).hasSize(2);
		assertThat(deleteEvents.stream().anyMatch(
				event -> ((AttachmentMarkAsDeletedEventContext) event.context()).getContentId()
															.equals(itemAttachmentEntityAfterChange.getContentId()))).isTrue();
		assertThat(deleteEvents.stream().anyMatch(
				event -> ((AttachmentMarkAsDeletedEventContext) event.context()).getContentId()
															.equals(itemAttachmentAfterChange.getContentId()))).isTrue();
	}

	@Override
	protected void verifyNumberOfEvents(String event, int number) {
		assertThat(serviceHandler.getEventContextForEvent(event)).hasSize(number);
	}

	@Override
	protected void verifyContentId(Attachments attachmentWithExpectedContent, String attachmentId, String contentId) {
		assertThat(attachmentWithExpectedContent.getContentId()).isNotEmpty().isNotEqualTo(contentId);
	}

	@Override
	protected void verifyContentAndContentId(Attachments attachment, String content, Attachments itemAttachment) {
		assertThat(attachment.getContent()).isNull();
		assertThat(attachment.getContentId()).isNotEmpty().isNotEqualTo(itemAttachment.getId());
	}

	@Override
	protected void verifyContentAndContentIdForAttachmentEntity(AttachmentEntity attachment, String content,
			AttachmentEntity itemAttachment) {
		assertThat(attachment.getContent()).isNull();
		assertThat(attachment.getContentId()).isNotEmpty().isNotEqualTo(itemAttachment.getId());
	}

	@Override
	protected void clearServiceHandlerContext() {
		serviceHandler.clearEventContext();
	}

	@Override
	protected void clearServiceHandlerDocuments() {
		serviceHandler.clearDocuments();
	}

	@Override
	protected void verifySingleCreateEvent(String contentId, String content) {
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT,
				AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
		var createEvent = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
		assertThat(createEvent).hasSize(1).first().satisfies(event -> {
			assertThat(event.context()).isInstanceOf(AttachmentCreateEventContext.class);
			var createContext = (AttachmentCreateEventContext) event.context();
			assertThat(createContext.getContentId()).isEqualTo(contentId);
			assertThat(createContext.getData().getContent().readAllBytes()).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
		});
	}

	@Override
	protected void verifySingleCreateAndUpdateEvent(String resultContentId, String toBeDeletedContentId, String content) {
		waitTillExpectedHandlerMessageSize(3);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
		var createEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
		assertThat(createEvents).hasSize(2);
		verifyCreateEventsContainsContentId(toBeDeletedContentId, createEvents);
		verifyCreateEventsContainsContentId(resultContentId, createEvents);
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);

		var deleteContentId = !resultContentId.equals(toBeDeletedContentId) ? toBeDeletedContentId : createEvents.stream()
																																																																																																	.filter(
																																																																																																			event -> !resultContentId.equals(
																																																																																																					((AttachmentCreateEventContext) event.context()).getContentId()))
																																																																																																	.findFirst()
																																																																																																	.orElseThrow()
																																																																																																	.context().get(
						Attachments.CONTENT_ID);

		var eventFound = deleteEvents.stream().anyMatch(
				event -> ((AttachmentMarkAsDeletedEventContext) event.context()).getContentId().equals(deleteContentId));
		assertThat(eventFound).isTrue();
	}

	@Override
	protected void verifySingleDeletionEvent(String contentId) {
		waitTillExpectedHandlerMessageSize(1);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
		var deleteEvents = serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
		assertThat(deleteEvents).hasSize(1).first().satisfies(event -> {
			assertThat(event.context()).isInstanceOf(AttachmentMarkAsDeletedEventContext.class);
			var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
			assertThat(deleteContext.getContentId()).isEqualTo(contentId);
		});
	}

	@Override
	protected void verifySingleReadEvent(String contentId) {
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT,
				AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
		var readContext = serviceHandler.getEventContext();
		assertThat(readContext).hasSize(1).first().satisfies(event -> {
			assertThat(event.event()).isEqualTo(AttachmentService.EVENT_READ_ATTACHMENT);
			assertThat(((AttachmentReadEventContext) event.context()).getContentId()).isEqualTo(contentId);
		});
	}

	@Override
	protected void verifyNoAttachmentEventsCalled() {
		assertThat(serviceHandler.getEventContext()).isEmpty();
	}

	@Override
	protected void verifyEventContextEmptyForEvent(String... events) {
		Arrays.stream(events).forEach(event -> assertThat(serviceHandler.getEventContextForEvent(event)).isEmpty());
	}

	private void verifyCreateEventsContainsContentId(String contentId, List<EventContextHolder> createEvents) {
		assertThat(createEvents.stream().anyMatch(
				event -> ((AttachmentCreateEventContext) event.context()).getContentId().equals(contentId))).isTrue();
	}

	private void waitTillExpectedHandlerMessageSize(int expectedSize) {
		Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
			var eventCalls = serviceHandler.getEventContext().size();
			logger.info("Waiting for expected size '{}' in handler context, was '{}'", expectedSize, eventCalls);
			var numberMatch = eventCalls >= expectedSize;
			if (!numberMatch) {
				serviceHandler.getEventContext().forEach(event -> logger.info("Event: {}", event));
			}
			return numberMatch;
		});
	}

}
