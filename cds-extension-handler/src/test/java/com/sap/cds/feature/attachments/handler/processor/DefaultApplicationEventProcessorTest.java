package com.sap.cds.feature.attachments.handler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Roots;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.runtime.CdsRuntime;

class DefaultApplicationEventProcessorTest {

		private static CdsRuntime runtime;
		private DefaultApplicationEventProcessor cut;
		private ApplicationEvent createApplicationEvent;
		private ApplicationEvent updateApplicationEvent;

		@BeforeAll
		static void classSetup() {
				runtime = new RuntimeHelper().runtime;
		}

		@BeforeEach
		void setup() {
				createApplicationEvent = mock(ApplicationEvent.class);
				updateApplicationEvent = mock(ApplicationEvent.class);
				cut = new DefaultApplicationEventProcessor(createApplicationEvent, updateApplicationEvent);
		}

		@Test
		void createEventReturned() {
				assertThat(cut.getApplicationEvent(CqnService.EVENT_CREATE)).isEqualTo(createApplicationEvent);
		}

		@Test
		void updateEventReturned() {
				assertThat(cut.getApplicationEvent(CqnService.EVENT_UPDATE)).isEqualTo(updateApplicationEvent);
		}

		@Test
		void wrongEventThrowsException() {
				assertThrows(IllegalStateException.class, () -> cut.getApplicationEvent("WRONG_EVENT"));
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

}
