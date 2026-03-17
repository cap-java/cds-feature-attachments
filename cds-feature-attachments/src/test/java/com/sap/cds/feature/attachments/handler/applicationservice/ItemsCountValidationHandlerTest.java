/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

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
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
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

class ItemsCountValidationHandlerTest {

  private static CdsRuntime runtime;

  private ItemsCountValidationHandler cut;
  private Messages messages;
  private Message messageBuilder;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    cut = new ItemsCountValidationHandler();
    messages = mock(Messages.class);
    messageBuilder = mock(Message.class);
    when(messages.warn(anyString(), anyInt())).thenReturn(messageBuilder);
    when(messages.error(anyString(), anyInt())).thenReturn(messageBuilder);
    when(messageBuilder.target(anyString(), any())).thenReturn(messageBuilder);
  }

  // --- CREATE handler tests ---

  @Test
  void createMaxItemsViolated_activeEntity_error() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(11); // MaxItems is 10

    cut.processCreateBefore(context, List.of(root));

    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void createMaxItemsNotViolated_noMessages() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(5); // MaxItems is 10, ok

    cut.processCreateBefore(context, List.of(root));

    verify(messages, never()).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void createMinItemsViolated_activeEntity_error() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(List.of()); // MinItems is 1, empty violates

    cut.processCreateBefore(context, List.of(root));

    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void createMinItemsNotViolated_noMessages() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(2); // MinItems is 1, ok

    cut.processCreateBefore(context, List.of(root));

    verify(messages, never()).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void createMinItemsViolated_draftData_warning() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(List.of()); // MinItems is 1
    root.put("IsActiveEntity", false); // marks as draft

    cut.processCreateBefore(context, List.of(root));

    verify(messages).warn(anyString(), anyInt());
    verify(messages, never()).error(anyString(), anyInt());
  }

  @Test
  void createMaxItemsViolated_draftData_warning() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(11); // MaxItems is 10
    root.put("IsActiveEntity", false);

    cut.processCreateBefore(context, List.of(root));

    verify(messages).warn(anyString(), anyInt());
    verify(messages, never()).error(anyString(), anyInt());
  }

  @Test
  void createCompositionAbsentFromPayload_noMessages() {
    var context = mockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create(); // attachments key not set at all

    cut.processCreateBefore(context, List.of(root));

    verify(messages, never()).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  // --- UPDATE handler tests ---

  @Test
  void updateMaxItemsViolated_activeEntity_error() {
    var context = mockUpdateContext(RootTable_.CDS_NAME);
    var root = rootWithAttachments(11);

    cut.processUpdateBefore(context, List.of(root));

    verify(messages).error(anyString(), anyInt());
    verify(messages, never()).warn(anyString(), anyInt());
  }

  @Test
  void updateMinItemsViolated_draftData_warning() {
    var context = mockUpdateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(List.of());
    root.put("IsActiveEntity", false);

    cut.processUpdateBefore(context, List.of(root));

    verify(messages).warn(anyString(), anyInt());
    verify(messages, never()).error(anyString(), anyInt());
  }

  // --- Annotation verification tests ---

  @Test
  void classHasCorrectAnnotation() {
    var annotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(annotation.type()).containsOnly(ApplicationService.class);
    assertThat(annotation.value()).containsOnly("*");
  }

  @Test
  void processCreateBeforeHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("processCreateBefore", CdsCreateEventContext.class, List.class);

    assertThat(method.getAnnotation(Before.class)).isNotNull();
    assertThat(method.getAnnotation(HandlerOrder.class).value()).isEqualTo(HandlerOrder.LATE);
  }

  @Test
  void processUpdateBeforeHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("processUpdateBefore", CdsUpdateEventContext.class, List.class);

    assertThat(method.getAnnotation(Before.class)).isNotNull();
    assertThat(method.getAnnotation(HandlerOrder.class).value()).isEqualTo(HandlerOrder.LATE);
  }

  // --- Helpers ---

  private CdsCreateEventContext mockCreateContext(String cdsName) {
    CdsEntity entity = runtime.getCdsModel().findEntity(cdsName).orElseThrow();
    CdsCreateEventContext context = mock(CdsCreateEventContext.class);
    when(context.getTarget()).thenReturn(entity);
    when(context.getMessages()).thenReturn(messages);
    return context;
  }

  private CdsUpdateEventContext mockUpdateContext(String cdsName) {
    CdsEntity entity = runtime.getCdsModel().findEntity(cdsName).orElseThrow();
    CdsUpdateEventContext context = mock(CdsUpdateEventContext.class);
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
