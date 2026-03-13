/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemCountValidatorTest {

  private static final String EVENT_ITEMS_ENTITY = "unit.test.TestService.EventItems";

  private static CdsRuntime runtime;

  private EventContext eventContext;
  private Messages messages;
  private Message messageMock;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    eventContext = mock(EventContext.class);
    messages = mock(Messages.class);
    messageMock = mock(Message.class);
    when(eventContext.getMessages()).thenReturn(messages);
    when(messageMock.target(org.mockito.ArgumentMatchers.anyString())).thenReturn(messageMock);
    when(messages.warn(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Object[].class)))
        .thenReturn(messageMock);
    when(messages.error(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Object[].class)))
        .thenReturn(messageMock);
  }

  // -------------------------------------------------------------------------
  // hasItemCountAnnotation
  // -------------------------------------------------------------------------

  @Test
  void hasItemCountAnnotation_minItems_returnsTrue() {
    CdsEntity entity = getEventItemsEntity();
    var comp = entity.findElement("minMaxAttachments").orElseThrow();
    assertThat(ItemCountValidator.hasItemCountAnnotation(comp)).isTrue();
  }

  @Test
  void hasItemCountAnnotation_maxItemsOnly_returnsTrue() {
    CdsEntity entity = getEventItemsEntity();
    var comp = entity.findElement("maxAttachments").orElseThrow();
    assertThat(ItemCountValidator.hasItemCountAnnotation(comp)).isTrue();
  }

  @Test
  void hasItemCountAnnotation_noAnnotation_returnsFalse() {
    CdsEntity entity = getEventItemsEntity();
    var comp = entity.findElement("sizeLimitedAttachments").orElseThrow();
    assertThat(ItemCountValidator.hasItemCountAnnotation(comp)).isFalse();
  }

  // -------------------------------------------------------------------------
  // resolveIntValue
  // -------------------------------------------------------------------------

  @Test
  void resolveIntValue_numericLiteral_returnsValue() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", 5);
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of());
    assertThat(result).contains(5L);
  }

  @Test
  void resolveIntValue_intStringLiteral_returnsValue() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", "3");
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of());
    assertThat(result).contains(3L);
  }

  @Test
  void resolveIntValue_propertyRef_resolvedFromData() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", "stock");
    CdsData data = CdsData.create();
    data.put("stock", 7);
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of(data));
    assertThat(result).contains(7L);
  }

  @Test
  void resolveIntValue_propertyRefAsString_resolvedFromData() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", "stock");
    CdsData data = CdsData.create();
    data.put("stock", "4");
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of(data));
    assertThat(result).contains(4L);
  }

  @Test
  void resolveIntValue_nullValue_returnsEmpty() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", null);
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void resolveIntValue_unresolvableString_returnsEmpty() {
    CdsAnnotation<?> ann = mockAnnotation("Validation.MinItems", "not_a_number");
    var result = ItemCountValidator.resolveIntValue(ann, mock(CdsEntity.class), List.of());
    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // messageKey
  // -------------------------------------------------------------------------

  @Test
  void messageKey_buildsKeyWithEntitySimpleNameAndProperty() {
    CdsEntity entity = getEventItemsEntity();
    String key =
        ItemCountValidator.messageKey(ItemCountValidator.MSG_MIN_ITEMS, entity, "attachments");
    assertThat(key).isEqualTo("AttachmentMinItems_EventItems_attachments");
  }

  // -------------------------------------------------------------------------
  // validate – active (error) path
  // -------------------------------------------------------------------------

  @Test
  void validate_tooFewAttachments_activeModeThrowsError() {
    CdsEntity entity = getEventItemsEntity();

    // minMaxAttachments requires min 2; we pass 1 → should error
    CdsData root = buildEventItemsData("minMaxAttachments", 1);

    // Make messages.throwIfError() propagate the error
    org.mockito.Mockito.doThrow(new ServiceException("min items violated"))
        .when(messages)
        .throwIfError();

    assertThrows(
        ServiceException.class,
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  @Test
  void validate_tooManyAttachments_activeModeThrowsError() {
    CdsEntity entity = getEventItemsEntity();

    // minMaxAttachments has max 5; we pass 6 → should error
    CdsData root = buildEventItemsData("minMaxAttachments", 6);

    org.mockito.Mockito.doThrow(new ServiceException("max items violated"))
        .when(messages)
        .throwIfError();

    assertThrows(
        ServiceException.class,
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  @Test
  void validate_withinBounds_activeModeNoException() {
    CdsEntity entity = getEventItemsEntity();

    // minMaxAttachments requires [2,5]; we pass 3 → OK
    CdsData root = buildEventItemsData("minMaxAttachments", 3);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  // -------------------------------------------------------------------------
  // validate – draft (warning) path
  // -------------------------------------------------------------------------

  @Test
  void validate_tooFewAttachments_draftModeIssuesWarningOnly() {
    CdsEntity entity = getEventItemsEntity();

    // minMaxAttachments requires min 2; we pass 1 → should warn, NOT throw
    CdsData root = buildEventItemsData("minMaxAttachments", 1);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, true));

    org.mockito.Mockito.verify(messages)
        .warn(
            org.mockito.ArgumentMatchers.contains("AttachmentMinItems"),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq(2L));
  }

  @Test
  void validate_tooManyAttachments_draftModeIssuesWarningOnly() {
    CdsEntity entity = getEventItemsEntity();

    // maxAttachments has max 3; we pass 5 → should warn, NOT throw
    CdsData root = buildEventItemsData("maxAttachments", 5);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, true));

    org.mockito.Mockito.verify(messages)
        .warn(
            org.mockito.ArgumentMatchers.contains("AttachmentMaxItems"),
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(3L));
  }

  @Test
  void validate_propertyRefAnnotation_resolvedFromData() {
    CdsEntity entity = getEventItemsEntity();

    // propertyRefAttachments has @Validation.MinItems: 'stock'
    // If stock=2, we need at least 2 attachments; we pass 1 → warning in draft
    CdsData root = buildEventItemsData("propertyRefAttachments", 1);
    root.put("stock", 2);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, true));

    org.mockito.Mockito.verify(messages)
        .warn(
            org.mockito.ArgumentMatchers.contains("AttachmentMinItems"),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq(2L));
  }

  @Test
  void validate_propertyRefResolvedToZero_noViolation() {
    CdsEntity entity = getEventItemsEntity();

    // propertyRefAttachments has @Validation.MinItems: 'stock'
    // If stock=0, we need 0 attachments; we pass 0 → no violation
    CdsData root = buildEventItemsData("propertyRefAttachments", 0);
    root.put("stock", 0);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  @Test
  void validate_exactlyAtMinimum_noViolation() {
    CdsEntity entity = getEventItemsEntity();

    // minAttachments requires min 1; we pass exactly 1 → no violation
    CdsData root = buildEventItemsData("minAttachments", 1);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  @Test
  void validate_exactlyAtMaximum_noViolation() {
    CdsEntity entity = getEventItemsEntity();

    // maxAttachments allows max 3; we pass exactly 3 → no violation
    CdsData root = buildEventItemsData("maxAttachments", 3);

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));
  }

  @Test
  void validate_noAnnotatedCompositions_nothingHappens() {
    // Use a different entity (Roots) that has no min/max annotations
    CdsEntity entity =
        runtime.getCdsModel().findEntity("unit.test.TestService.RootTable").orElseThrow();
    CdsData root = CdsData.create();

    assertDoesNotThrow(
        () -> ItemCountValidator.validate(entity, List.of(root), eventContext, false));

    org.mockito.Mockito.verifyNoInteractions(messages);
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private CdsEntity getEventItemsEntity() {
    return runtime.getCdsModel().findEntity(EVENT_ITEMS_ENTITY).orElseThrow();
  }

  /**
   * Builds a synthetic {@link CdsData} entry for an EventItems record. The named composition has
   * {@code count} attachments. All other annotated compositions are pre-populated with data that
   * satisfies their constraints so that tests can focus on a single composition at a time.
   *
   * <p>Default pre-populated counts (chosen to be within all bounds):
   *
   * <ul>
   *   <li>{@code minMaxAttachments}: 3 (satisfies [2,5])
   *   <li>{@code minAttachments}: 1 (satisfies min=1)
   *   <li>{@code maxAttachments}: 0 (satisfies max=3)
   *   <li>{@code propertyRefAttachments}: 0 (no stock in data → constraint skipped)
   * </ul>
   */
  private static CdsData buildEventItemsData(String compositionName, int count) {
    // Default valid counts for all annotated compositions in EventItems
    java.util.Map<String, Integer> defaults = new java.util.LinkedHashMap<>();
    defaults.put("minMaxAttachments", 3);
    defaults.put("minAttachments", 1);
    defaults.put("maxAttachments", 0);
    defaults.put("propertyRefAttachments", 0);
    // Override the specific composition under test
    defaults.put(compositionName, count);

    CdsData root = CdsData.create();
    for (var entry : defaults.entrySet()) {
      List<CdsData> attachments = new ArrayList<>();
      for (int i = 0; i < entry.getValue(); i++) {
        Attachments att = Attachments.create();
        att.setId(UUID.randomUUID().toString());
        attachments.add(att);
      }
      root.put(entry.getKey(), attachments);
    }
    return root;
  }

  @SuppressWarnings("unchecked")
  private static CdsAnnotation<?> mockAnnotation(String name, Object value) {
    CdsAnnotation<Object> ann = mock(CdsAnnotation.class);
    when(ann.getName()).thenReturn(name);
    when(ann.getValue()).thenReturn(value);
    return ann;
  }
}
