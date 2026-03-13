/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ItemsCountViolation;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.utils.OrderConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ItemsCountValidationHandlerTest {

  private static CdsRuntime runtime;

  private ItemsCountValidationHandler cut;
  private AttachmentsReader attachmentsReader;
  private ThreadDataStorageReader storageReader;
  private CdsCreateEventContext createContext;
  private CdsUpdateEventContext updateContext;
  private Messages messages;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentsReader = mock(AttachmentsReader.class);
    storageReader = mock(ThreadDataStorageReader.class);
    cut = new ItemsCountValidationHandler(attachmentsReader, storageReader);

    createContext = mock(CdsCreateEventContext.class);
    updateContext = mock(CdsUpdateEventContext.class);
    messages = mock(Messages.class, Mockito.RETURNS_DEEP_STUBS);

    when(createContext.getMessages()).thenReturn(messages);
    when(updateContext.getMessages()).thenReturn(messages);
  }

  @Test
  void classHasCorrectAnnotation() {
    var annotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(annotation.type()).containsOnly(ApplicationService.class);
    assertThat(annotation.value()).containsOnly("*");
  }

  @Test
  void validateOnCreateMethodHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("validateOnCreate", CdsCreateEventContext.class, List.class);

    var beforeAnnotation = method.getAnnotation(Before.class);
    var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(beforeAnnotation).isNotNull();
    assertThat(handlerOrderAnnotation.value()).isEqualTo(OrderConstants.Before.CHECK_CAPABILITIES);
  }

  @Test
  void validateOnUpdateMethodHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("validateOnUpdate", CdsUpdateEventContext.class, List.class);

    var beforeAnnotation = method.getAnnotation(Before.class);
    var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(beforeAnnotation).isNotNull();
    assertThat(handlerOrderAnnotation.value()).isEqualTo(OrderConstants.Before.CHECK_CAPABILITIES);
  }

  @Test
  void createWithNoAnnotatedCompositionsDoesNothing() {
    // Attachment entity has no compositions with MaxItems/MinItems
    getEntityAndMockCreateContext(Attachment_.CDS_NAME);
    var attachment = Attachments.create();

    cut.validateOnCreate(createContext, List.of(attachment));

    verifyNoInteractions(storageReader);
    verifyNoInteractions(attachmentsReader);
  }

  @Test
  void createWithinLimitsNoViolation() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(createAttachments(5));
    when(storageReader.get()).thenReturn(false);

    assertDoesNotThrow(() -> cut.validateOnCreate(createContext, List.of(root)));
  }

  @Test
  void createExceedsMaxItemsThrowsError() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(createAttachments(25));
    when(storageReader.get()).thenReturn(false);

    var exception =
        assertThrows(
            ServiceException.class, () -> cut.validateOnCreate(createContext, List.of(root)));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.CONFLICT);
  }

  @Test
  void createBelowMinItemsThrowsError() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(createAttachments(1));
    when(storageReader.get()).thenReturn(false);

    var exception =
        assertThrows(
            ServiceException.class, () -> cut.validateOnCreate(createContext, List.of(root)));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.CONFLICT);
  }

  @Test
  void createInDraftModeAddsWarningInsteadOfError() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setAttachments(createAttachments(25));
    when(storageReader.get()).thenReturn(true);

    assertDoesNotThrow(() -> cut.validateOnCreate(createContext, List.of(root)));

    verify(messages).warn("AttachmentMaxItemsExceeded", 20, 25);
  }

  @Test
  void createWithNoCompositionInPayloadSkipsValidation() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setTitle("test");
    // attachments not in payload

    assertDoesNotThrow(() -> cut.validateOnCreate(createContext, List.of(root)));
    verifyNoInteractions(storageReader);
  }

  @Test
  void updateWithinLimitsNoViolation() {
    var id = getEntityAndMockUpdateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setId(id);
    root.setAttachments(createAttachments(10));
    when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class)))
        .thenReturn(Collections.emptyList());
    when(storageReader.get()).thenReturn(false);

    assertDoesNotThrow(() -> cut.validateOnUpdate(updateContext, List.of(root)));
  }

  @Test
  void updateExceedsMaxItemsThrowsError() {
    var id = getEntityAndMockUpdateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setId(id);
    root.setAttachments(createAttachments(25));
    when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class)))
        .thenReturn(Collections.emptyList());
    when(storageReader.get()).thenReturn(false);

    var exception =
        assertThrows(
            ServiceException.class, () -> cut.validateOnUpdate(updateContext, List.of(root)));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.CONFLICT);
  }

  @Test
  void updateInDraftModeAddsWarning() {
    var id = getEntityAndMockUpdateContext(RootTable_.CDS_NAME);
    var root = RootTable.create();
    root.setId(id);
    root.setAttachments(createAttachments(25));
    when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class)))
        .thenReturn(Collections.emptyList());
    when(storageReader.get()).thenReturn(true);

    assertDoesNotThrow(() -> cut.validateOnUpdate(updateContext, List.of(root)));

    verify(messages).warn("AttachmentMaxItemsExceeded", 20, 25);
  }

  @Test
  void reportViolationsThrowsErrorWhenNotDraft() {
    var violations =
        List.of(
            new ItemsCountViolation(
                ItemsCountViolation.Type.MAX_ITEMS, "attachments", "RootTable", 25, 20));

    var exception =
        assertThrows(
            ServiceException.class,
            () -> ItemsCountValidationHandler.reportViolations(violations, messages, false));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.CONFLICT);
  }

  @Test
  void reportViolationsAddsWarningWhenDraft() {
    var violations =
        List.of(
            new ItemsCountViolation(
                ItemsCountViolation.Type.MAX_ITEMS, "attachments", "RootTable", 25, 20));

    assertDoesNotThrow(
        () -> ItemsCountValidationHandler.reportViolations(violations, messages, true));

    verify(messages).warn("AttachmentMaxItemsExceeded", 20, 25);
  }

  @Test
  void hasCompositionsWithItemCountAnnotationsReturnsTrueForAnnotatedEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    assertThat(ItemsCountValidationHandler.hasCompositionsWithItemCountAnnotations(entity))
        .isTrue();
  }

  @Test
  void hasCompositionsWithItemCountAnnotationsReturnsFalseForNonAnnotatedEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    assertThat(ItemsCountValidationHandler.hasCompositionsWithItemCountAnnotations(entity))
        .isFalse();
  }

  @Test
  void hasCompositionsWithItemCountAnnotationsReturnsTrueForMinItemsOnlyEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();
    assertThat(ItemsCountValidationHandler.hasCompositionsWithItemCountAnnotations(entity))
        .isTrue();
  }

  @Test
  void updateWithNoAnnotatedCompositionsDoesNothing() {
    // Attachment entity has no compositions with MaxItems/MinItems
    var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    when(updateContext.getTarget()).thenReturn(serviceEntity);
    var attachment = Attachments.create();

    cut.validateOnUpdate(updateContext, List.of(attachment));

    verifyNoInteractions(storageReader);
    verifyNoInteractions(attachmentsReader);
  }

  @Test
  void createWithEmptyViolationsListDoesNothing() {
    getEntityAndMockCreateContext(RootTable_.CDS_NAME);
    // No composition in payload means no violations
    var root = RootTable.create();
    root.setTitle("test");

    cut.validateOnCreate(createContext, List.of(root));

    verifyNoInteractions(storageReader);
  }

  @Test
  void reportViolationsMinItemsAddsWarningWhenDraft() {
    var violations =
        List.of(
            new ItemsCountViolation(
                ItemsCountViolation.Type.MIN_ITEMS, "attachments", "RootTable", 1, 2));

    assertDoesNotThrow(
        () -> ItemsCountValidationHandler.reportViolations(violations, messages, true));

    verify(messages).warn("AttachmentMinItemsNotReached", 2, 1);
  }

  @Test
  void reportViolationsMinItemsThrowsErrorWhenNotDraft() {
    var violations =
        List.of(
            new ItemsCountViolation(
                ItemsCountViolation.Type.MIN_ITEMS, "attachments", "RootTable", 1, 2));

    var exception =
        assertThrows(
            ServiceException.class,
            () -> ItemsCountValidationHandler.reportViolations(violations, messages, false));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.CONFLICT);
  }

  private void getEntityAndMockCreateContext(String cdsName) {
    var serviceEntity = runtime.getCdsModel().findEntity(cdsName).orElseThrow();
    when(createContext.getTarget()).thenReturn(serviceEntity);
  }

  private String getEntityAndMockUpdateContext(String cdsName) {
    var serviceEntity = runtime.getCdsModel().findEntity(cdsName).orElseThrow();
    var id = UUID.randomUUID().toString();
    CqnUpdate update =
        Update.entity(serviceEntity.getQualifiedName()).where(entity -> entity.get("ID").eq(id));
    when(updateContext.getTarget()).thenReturn(serviceEntity);
    when(updateContext.getModel()).thenReturn(runtime.getCdsModel());
    when(updateContext.getCqn()).thenReturn(update);
    return id;
  }

  private List<Attachments> createAttachments(int count) {
    List<Attachments> attachments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      var attachment = Attachments.create();
      attachment.setFileName("file" + i + ".txt");
      attachments.add(attachment);
    }
    return attachments;
  }
}
