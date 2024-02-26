package com.sap.cds.feature.attachments.handler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Roots;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.runtime.CdsRuntime;

class DefaultApplicationEventProcessorTest {

		private DefaultApplicationEventProcessor cut;

		private static CdsRuntime runtime;

		@BeforeAll
		static void classSetup() {
				runtime = new RuntimeHelper().runtime;
		}

		@BeforeEach
		void setup() {
				AttachmentService attachmentService = mock(AttachmentService.class);
				cut = new DefaultApplicationEventProcessor(attachmentService);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void updateEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();
				cdsData.put(fieldNames.documentIdField().get(), "documentId");

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isInstanceOf(UpdateEvent.class);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void updateEventReturnedIDocumentFieldNameNotPresent(String eventName) {
				var fieldNames = new AttachmentFieldNames("key", Optional.empty(), Optional.of("mimeType"), Optional.of("fileName"));
				var cdsData = CdsData.create();
				cdsData.put("documentID", "documentId");

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isInstanceOf(StoreEvent.class);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void storeEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isInstanceOf(StoreEvent.class);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void deleteEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				var event = cut.getEvent(eventName, null, fieldNames, cdsData);

				assertThat(event).isInstanceOf(DeleteContentEvent.class);
		}

		@Test
		void exceptionThrownIfWrongEventType() {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				assertThrows(IllegalStateException.class, () -> cut.getEvent("WRONG_EVENT", null, fieldNames, cdsData));
		}

		@Test
		void processingIsNeededForAttachmentEntity() {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setContent(null);

				assertThat(cut.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).isTrue();
		}

		@Test
		void processingIsNotNeededForAttachmentEntityWithoutContent() {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();

				assertThat(cut.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).isFalse();
		}

		@Test
		void processingIsNeededForParentEntityWithAttachment() {
				var serviceEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setContent(null);
				var root = Roots.create();
				root.setAttachments(List.of(attachment));

				assertThat(cut.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(root))).isTrue();
		}

		@Test
		void processingIsNotNeededForParentEntityWithAttachmentButWithoutContent() {
				var serviceEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME);
				var attachment = Attachment.create();
				var root = Roots.create();
				root.setAttachments(List.of(attachment));

				assertThat(cut.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(root))).isFalse();
		}

		@Test
		void processingIsNotNeededForParentEntityWithoutAttachment() {
				var serviceEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME);
				var root = Roots.create();
				root.setId("id");

				assertThat(cut.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(root))).isFalse();
		}

		private AttachmentFieldNames getDefaultFieldNames() {
				return new AttachmentFieldNames("key", Optional.of("documentID"), Optional.of("mimeType"), Optional.of("fileName"));
		}

}
