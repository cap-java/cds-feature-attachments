/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DraftSaveItemsCountValidationHandlerTest {

  private static CdsRuntime runtime;

  private DraftSaveItemsCountValidationHandler cut;
  private Messages messages;
  private Message messageBuilder;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    cut = new DraftSaveItemsCountValidationHandler();
    messages = mock(Messages.class);
    messageBuilder = mock(Message.class);
    when(messages.warn(anyString(), anyInt())).thenReturn(messageBuilder);
    when(messages.error(anyString(), anyInt())).thenReturn(messageBuilder);
    when(messageBuilder.target(anyString(), any())).thenReturn(messageBuilder);
  }

  @Test
  void maxItemsViolated_alwaysError() {
    var context = mockContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(11); // MaxItems is 10

    cut.processBeforeDraftSave(context, List.of(root));

    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void maxItemsViolated_evenIfDraftFlagged_stillError() {
    var context = mockContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(11);
    root.put("IsActiveEntity", false); // draft flag in data — ignored by this handler

    cut.processBeforeDraftSave(context, List.of(root));

    // DraftSave always errors regardless of IsActiveEntity flag
    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void minItemsViolated_alwaysError() {
    var context = mockContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(List.of()); // MinItems is 1

    cut.processBeforeDraftSave(context, List.of(root));

    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void noViolation_noMessages() {
    var context = mockContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(5); // MinItems:1, MaxItems:10 → ok

    cut.processBeforeDraftSave(context, List.of(root));

    verify(messages, never()).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void compositionAbsentFromPayload_noMessages() {
    var context = mockContext(RootTable_.CDS_NAME);
    var root = RootTable.create(); // attachments not set in payload

    cut.processBeforeDraftSave(context, List.of(root));

    verify(messages, never()).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void classHasCorrectAnnotation() {
    var annotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(annotation.type()).containsOnly(DraftService.class);
    assertThat(annotation.value()).containsOnly("*");
  }

  @Test
  void processBeforeDraftSaveHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("processBeforeDraftSave", DraftSaveEventContext.class, List.class);

    assertThat(method.getAnnotation(Before.class)).isNotNull();
    assertThat(method.getAnnotation(HandlerOrder.class).value()).isEqualTo(HandlerOrder.LATE);
  }

  // --- Helpers ---

  private DraftSaveEventContext mockContext(String cdsName) {
    CdsEntity entity = runtime.getCdsModel().findEntity(cdsName).orElseThrow();
    DraftSaveEventContext context = mock(DraftSaveEventContext.class);
    when(context.getTarget()).thenReturn(entity);
    when(context.getMessages()).thenReturn(messages);
    return context;
  }

  private RootTable rootWithAttachments(int count) {
    var root = RootTable.create();
    List<CdsData> attachments =
        IntStream.range(0, count)
            .mapToObj(i -> (CdsData) Attachments.create())
            .collect(Collectors.toList());
    root.setAttachments(attachments);
    return root;
  }
}
