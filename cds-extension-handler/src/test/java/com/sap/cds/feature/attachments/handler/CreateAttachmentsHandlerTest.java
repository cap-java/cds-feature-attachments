package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;

class CreateAttachmentsHandlerTest extends ModifyApplicationEventTestBase {

		private CreateAttachmentsHandler cut;
		private CdsCreateEventContext createContext;

		@BeforeAll
		static void classSetup() {
				runtime = RuntimeHelper.runtime;
		}

		@BeforeEach
		void setup() {
				super.setup();
				cut = new CreateAttachmentsHandler(persistenceService, eventFactory);

				createContext = mock(CdsCreateEventContext.class);
		}

		@Test
		void noContentInDataNothingToDo() {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				mockTargetInContext(serviceEntity.orElseThrow());

				cut.processAfter(createContext, List.of(attachment));

				verifyNoInteractions(persistenceService);
				verifyNoInteractions(eventFactory);
		}

		@Test
		void idsAreSetInDataForCreate() {
				var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
				var roots = RootTable.create();
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				roots.setAttachmentTable(List.of(attachment));
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);

				cut.processAfter(createContext, List.of(roots));

				assertThat(roots.getId()).isNotEmpty();
				assertThat(attachment.getId()).isNotEmpty();
		}

		@Test
		void eventProcessorCalledForCreate() throws IOException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);

				try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
						var attachment = Attachment.create();
						attachment.setContent(testStream);
						mockTargetInContext(serviceEntity.orElseThrow());
						when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
						var row = mockSelectionResult();

						cut.processAfter(createContext, List.of(attachment));

						verify(eventFactory).getEvent(eq(CqnService.EVENT_CREATE), eq(testStream), fieldNamesArgumentCaptor.capture(), eq(row));
						verifyFilledFieldNames();
				}
		}

		@Test
		void attachmentAccessExceptionCorrectHandledForCreate() {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
				when(event.processEvent(any(), any(), any(), any(), any(), any())).thenThrow(new ServiceException(""));

				List<CdsData> input = List.of(attachment);
				assertThrows(ServiceException.class, () -> cut.processAfter(createContext, input));
		}

		@Test
		void checkAnnotations() {
				fail("not implemented");
		}

		private void mockTargetInContext(CdsEntity serviceEntity) {
				when(createContext.getTarget()).thenReturn(serviceEntity);
		}

}
