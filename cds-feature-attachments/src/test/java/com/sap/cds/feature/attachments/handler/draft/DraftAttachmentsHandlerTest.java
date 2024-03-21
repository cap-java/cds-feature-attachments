package com.sap.cds.feature.attachments.handler.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.handler.draftservice.DraftAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;

class DraftAttachmentsHandlerTest {

	private static CdsRuntime runtime;
	private DraftAttachmentsHandler cut;
	private EventContext eventContext;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		cut = new DraftAttachmentsHandler();
		eventContext = mock(EventContext.class);
	}

	@Test
	void documentIdIsSetToNullForMediaEntity() {
		getEntityAndMockContext(Items_.CDS_NAME);
		var items = Items.create();
		var attachments = Attachments.create();
		attachments.setContent(mock(InputStream.class));
		items.setAttachments(List.of(attachments));

		cut.processBeforeDraftPatch(eventContext, List.of(items));

		assertThat(attachments).containsEntry(Attachments.DOCUMENT_ID, null);
	}

	@Test
	void documentIdIsNotSetForNonMediaEntity() {
		getEntityAndMockContext(Events_.CDS_NAME);
		var events = Events.create();
		events.setContent("test");

		cut.processBeforeDraftPatch(eventContext, List.of(events));

		assertThat(events).doesNotContainKey(Attachments.DOCUMENT_ID);
	}

	@Test
	void classHasCorrectAnnotations() {
		var serviceAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(serviceAnnotation.value()).containsOnly("*");
		assertThat(serviceAnnotation.type()).containsOnly(DraftService.class);
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processBeforeDraftPatch", EventContext.class, List.class);
		var beforeAnnotation = method.getAnnotation(Before.class);
		var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(beforeAnnotation.event()).containsOnly(DraftService.EVENT_DRAFT_PATCH);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
	}

	private void getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		mockTargetInUpdateContext(serviceEntity.orElseThrow());
	}

	private void mockTargetInUpdateContext(CdsEntity serviceEntity) {
		when(eventContext.getTarget()).thenReturn(serviceEntity);
	}

}
