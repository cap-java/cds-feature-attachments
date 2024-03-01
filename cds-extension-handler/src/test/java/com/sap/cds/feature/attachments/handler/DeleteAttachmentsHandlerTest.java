package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

class DeleteAttachmentsHandlerTest {

		private DeleteAttachmentsHandler cut;

		@BeforeEach
		void setup() {
				cut = new DeleteAttachmentsHandler();
		}

		@Test
		void dummy() {
				//TODO remove if logic is implemented
				cut.processAfter(mock(CdsDeleteEventContext.class));
		}

		@Test
		void classHasCorrectAnnotation() {
				var deleteHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

				assertThat(deleteHandlerAnnotation.type()).containsOnly(ApplicationService.class);
				assertThat(deleteHandlerAnnotation.value()).containsOnly("*");
		}

		@Test
		void methodHasCorrectAnnotations() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("processAfter", CdsDeleteEventContext.class);

				var deleteAfterAnnotation = method.getAnnotation(After.class);
				var deleteHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(deleteAfterAnnotation.event()).containsOnly(CqnService.EVENT_DELETE);
				assertThat(deleteHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

}
