/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemsCountValidatorTest {

  private static CdsRuntime runtime;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @Test
  void noViolationsWhenCompositionNotInPayload() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    root.setTitle("test");
    // attachments not in payload

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).isEmpty();
  }

  @Test
  void noViolationsWhenWithinLimits() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    // Create 5 attachments (within 2-20 range)
    List<Attachments> attachments = createAttachments(5);
    root.setAttachments(attachments);

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).isEmpty();
  }

  @Test
  void maxItemsViolation() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    // Create 25 attachments (exceeds max of 20)
    List<Attachments> attachments = createAttachments(25);
    root.setAttachments(attachments);

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).type()).isEqualTo(ItemsCountViolation.Type.MAX_ITEMS);
    assertThat(violations.get(0).compositionName()).isEqualTo("attachments");
    assertThat(violations.get(0).actualCount()).isEqualTo(25);
    assertThat(violations.get(0).limit()).isEqualTo(20);
  }

  @Test
  void minItemsViolation() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    // Create 1 attachment (below min of 2)
    List<Attachments> attachments = createAttachments(1);
    root.setAttachments(attachments);

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).type()).isEqualTo(ItemsCountViolation.Type.MIN_ITEMS);
    assertThat(violations.get(0).compositionName()).isEqualTo("attachments");
    assertThat(violations.get(0).actualCount()).isEqualTo(1);
    assertThat(violations.get(0).limit()).isEqualTo(2);
  }

  @Test
  void emptyAttachmentsViolatesMinItems() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    root.setAttachments(Collections.emptyList());

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).type()).isEqualTo(ItemsCountViolation.Type.MIN_ITEMS);
    assertThat(violations.get(0).actualCount()).isEqualTo(0);
  }

  @Test
  void exactMinLimitNoViolation() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    // Create exactly 2 attachments (min is 2)
    List<Attachments> attachments = createAttachments(2);
    root.setAttachments(attachments);

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).isEmpty();
  }

  @Test
  void exactMaxLimitNoViolation() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    // Create exactly 20 attachments (max is 20)
    List<Attachments> attachments = createAttachments(20);
    root.setAttachments(attachments);

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    assertThat(violations).isEmpty();
  }

  @Test
  void violationMessageKeys() {
    var maxViolation =
        new ItemsCountViolation(
            ItemsCountViolation.Type.MAX_ITEMS, "attachments", "RootTable", 25, 20);

    assertThat(maxViolation.getBaseMessageKey()).isEqualTo("AttachmentMaxItemsExceeded");
    assertThat(maxViolation.getOverrideMessageKey())
        .isEqualTo("AttachmentMaxItemsExceeded_RootTable_attachments");

    var minViolation =
        new ItemsCountViolation(
            ItemsCountViolation.Type.MIN_ITEMS, "attachments", "RootTable", 1, 2);

    assertThat(minViolation.getBaseMessageKey()).isEqualTo("AttachmentMinItemsNotReached");
    assertThat(minViolation.getOverrideMessageKey())
        .isEqualTo("AttachmentMinItemsNotReached_RootTable_attachments");
  }

  @Test
  void resolveAnnotationValueWithPropertyReference() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    // The @Validation.MaxItems: 20 annotation should be resolvable
    var element = entity.findElement("attachments").orElseThrow();
    var parentData = CdsData.create();
    parentData.put("someField", 15);

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, parentData);

    assertThat(result).isPresent().hasValue(20);
  }

  @Test
  void resolveAnnotationValueReturnsEmptyWhenAnnotationNotPresent() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.empty());

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, Map.of());

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsEmptyForNullAnnotationValue() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn(null);
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, Map.of());

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsEmptyForBareAnnotation() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn(true);
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, Map.of());

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsValueForNumberType() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn(42);
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, Map.of());

    assertThat(result).isPresent().hasValue(42);
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueParsesStringInteger() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("15");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, Map.of());

    assertThat(result).isPresent().hasValue(15);
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueResolvesPropertyReferenceFromNumber() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("maxCount");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));
    Map<String, Object> parentData = new HashMap<>();
    parentData.put("maxCount", 30);

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, parentData);

    assertThat(result).isPresent().hasValue(30);
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueResolvesPropertyReferenceFromStringNumber() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("maxCount");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));
    Map<String, Object> parentData = new HashMap<>();
    parentData.put("maxCount", "25");

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, parentData);

    assertThat(result).isPresent().hasValue(25);
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsEmptyForUnresolvablePropertyValue() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("maxCount");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));
    Map<String, Object> parentData = new HashMap<>();
    parentData.put("maxCount", "not-a-number");

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, parentData);

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsEmptyForMissingPropertyInData() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("nonExistentProp");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));
    Map<String, Object> parentData = new HashMap<>();

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, parentData);

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveAnnotationValueReturnsEmptyForNullParentData() {
    CdsElementDefinition element = mock(CdsElementDefinition.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn("maxCount");
    when(element.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.of(annotation));

    var result =
        ItemsCountValidator.resolveAnnotationValue(
            element, ItemsCountValidator.ANNOTATION_MAX_ITEMS, null);

    assertThat(result).isEmpty();
  }

  @Test
  void validateWithNullExistingDataList() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    root.setAttachments(createAttachments(25));

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), null);

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).type()).isEqualTo(ItemsCountViolation.Type.MAX_ITEMS);
  }

  @Test
  void validateWithExistingDataShorterThanNewData() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root1 = RootTable.create();
    root1.setAttachments(createAttachments(5));
    var root2 = RootTable.create();
    root2.setAttachments(createAttachments(25));

    // Existing data has only one entry, but new data has two
    var existingRoot = RootTable.create();
    existingRoot.setTitle("existing");

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root1, root2), List.of(existingRoot));

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).actualCount()).isEqualTo(25);
  }

  @Test
  void validateWithNonCollectionCompositionData() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = CdsData.create();
    // Set composition as non-collection (e.g., a string)
    root.put("attachments", "not-a-collection");

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), new ArrayList<>());

    // Should skip validation when composition data is not a collection
    assertThat(violations).isEmpty();
  }

  @Test
  void validateMergesExistingDataWithRequestData() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var root = RootTable.create();
    root.setAttachments(createAttachments(5));

    var existingRoot = RootTable.create();
    existingRoot.setTitle("existing");

    // Validate with existing data to ensure merge works
    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(root), List.of(existingRoot));

    assertThat(violations).isEmpty();
  }

  @Test
  void minItemsOnlyAnnotationViolation() {
    // Items entity has only @Validation.MinItems on itemAttachments (no MaxItems)
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();
    var item = Items.create();
    item.setItemAttachments(Collections.emptyList());

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(item), new ArrayList<>());

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).type()).isEqualTo(ItemsCountViolation.Type.MIN_ITEMS);
    assertThat(violations.get(0).limit()).isEqualTo(1);
  }

  @Test
  void minItemsOnlyAnnotationNoViolation() {
    // Items entity has only @Validation.MinItems on itemAttachments (no MaxItems)
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();
    var item = Items.create();
    item.setItemAttachments(createAttachments(5));

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(entity, List.of(item), new ArrayList<>());

    assertThat(violations).isEmpty();
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
