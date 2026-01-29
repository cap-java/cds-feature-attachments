/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttachmentCountValidatorTest {

  private AttachmentCountValidator cut;

  @BeforeEach
  void setup() {
    cut = new AttachmentCountValidator();
  }

  private CdsEntity getCountValidatedEntity() {
    return RuntimeHelper.runtime
        .getCdsModel()
        .findEntity("unit.test.CountValidatedEntity")
        .orElseThrow();
  }

  private List<CdsData> createDataWithAttachments(String compositionName, int count) {
    List<Map<String, Object>> attachmentsList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> attachment = new HashMap<>();
      attachment.put("ID", "attachment-" + i);
      attachment.put("filename", "file" + i + ".txt");
      attachmentsList.add(attachment);
    }

    CdsData data = CdsData.create();
    data.put("ID", "entity-1");
    data.put(compositionName, attachmentsList);

    return List.of(data);
  }

  @Nested
  class ValidateForCreate {

    @Test
    void maxOnlyAttachments_underLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void maxOnlyAttachments_atLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void maxOnlyAttachments_overLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 4);

      assertThatThrownBy(() -> cut.validateForCreate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void minOnlyAttachments_zeroItems_throwsException() {
      // MinItems is validated on CREATE for non-draft operations
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minOnlyAttachments", 0);

      assertThatThrownBy(() -> cut.validateForCreate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void minOnlyAttachments_atMinLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minOnlyAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void minMaxAttachments_underMinLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 1);

      assertThatThrownBy(() -> cut.validateForCreate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void minMaxAttachments_atMinLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void minMaxAttachments_atMaxLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void minMaxAttachments_inRange_noException() {
      CdsEntity entity = getCountValidatedEntity();
      // 2 is at the min limit but within range
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void minMaxAttachments_overMaxLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 4);

      assertThatThrownBy(() -> cut.validateForCreate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void unlimitedAttachments_anyCount_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("unlimitedAttachments", 100);

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void emptyData_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = List.of();

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, data));
    }

    @Test
    void noCompositionInData_noException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsData data = CdsData.create();
      data.put("ID", "entity-1");
      data.put("title", "Test");
      // No composition field

      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, List.of(data)));
    }
  }

  @Nested
  class ValidateForUpdate {

    @Test
    void maxOnlyAttachments_underLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void maxOnlyAttachments_atLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void maxOnlyAttachments_overLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 4);

      assertThatThrownBy(() -> cut.validateForUpdate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void minOnlyAttachments_underLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minOnlyAttachments", 1);

      assertThatThrownBy(() -> cut.validateForUpdate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void minOnlyAttachments_atLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minOnlyAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void minOnlyAttachments_overLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minOnlyAttachments", 5);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void minMaxAttachments_underMinLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 1);

      assertThatThrownBy(() -> cut.validateForUpdate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void minMaxAttachments_atMinLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void minMaxAttachments_inRange_noException() {
      CdsEntity entity = getCountValidatedEntity();
      // 2 and 3 are both valid
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);
      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));

      List<CdsData> data2 = createDataWithAttachments("minMaxAttachments", 3);
      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data2));
    }

    @Test
    void minMaxAttachments_atMaxLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void minMaxAttachments_overMaxLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 4);

      assertThatThrownBy(() -> cut.validateForUpdate(entity, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void unlimitedAttachments_anyCount_noException() {
      CdsEntity entity = getCountValidatedEntity();
      List<CdsData> data = createDataWithAttachments("unlimitedAttachments", 100);

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, data));
    }

    @Test
    void compositionNotInRequest_noValidation() {
      CdsEntity entity = getCountValidatedEntity();
      // Data doesn't contain any composition fields
      CdsData data = CdsData.create();
      data.put("ID", "entity-1");
      data.put("title", "Updated title");

      assertThatNoException().isThrownBy(() -> cut.validateForUpdate(entity, List.of(data)));
    }
  }

  @Nested
  class ValidateForDraftSave {

    private static final String COUNT_VALIDATED_ENTITY = "unit.test.CountValidatedEntity";
    private static final String ROOTS_ENTITY = "unit.test.Roots";

    private CqnService service;
    private Result result;

    @BeforeEach
    void setupMocks() {
      service = mock(CqnService.class);
      result = mock(Result.class);
      when(service.run(any(CqnSelect.class))).thenReturn(result);
    }

    private CdsEntity getEntityWithoutValidation() {
      return RuntimeHelper.runtime.getCdsModel().findEntity(ROOTS_ENTITY).orElseThrow();
    }

    private List<CdsData> createParentDataWithComposition(
        String compositionName, int attachmentCount) {
      List<Map<String, Object>> attachments = new ArrayList<>();
      for (int i = 0; i < attachmentCount; i++) {
        attachments.add(Map.of("ID", "att-" + i));
      }
      CdsData parent = CdsData.create();
      parent.put(compositionName, attachments);
      return List.of(parent);
    }

    /** Creates parent data with valid counts for all validated compositions. */
    private List<CdsData> createValidParentData() {
      CdsData parent = CdsData.create();
      // maxOnlyAttachments: max=3, so 2 is valid
      parent.put("maxOnlyAttachments", List.of(Map.of("ID", "att-1"), Map.of("ID", "att-2")));
      // minOnlyAttachments: min=2, so 2 is valid
      parent.put("minOnlyAttachments", List.of(Map.of("ID", "att-3"), Map.of("ID", "att-4")));
      // minMaxAttachments: min=2, max=3, so 2 is valid
      parent.put("minMaxAttachments", List.of(Map.of("ID", "att-5"), Map.of("ID", "att-6")));
      return List.of(parent);
    }

    @Test
    void noValidatedCompositions_returnsEarly() {
      CdsEntity entity = getEntityWithoutValidation();
      Select<?> statement = Select.from(ROOTS_ENTITY);

      cut.validateForDraftSave(entity, statement, service);

      verifyNoInteractions(service);
    }

    @Test
    void validCount_noException() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      List<CdsData> parentData = createValidParentData();
      when(result.listOf(CdsData.class)).thenReturn(parentData);

      assertThatNoException().isThrownBy(() -> cut.validateForDraftSave(entity, statement, service));

      verify(service).run(any(CqnSelect.class));
    }

    @Test
    void maxExceeded_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      List<CdsData> parentData = createParentDataWithComposition("maxOnlyAttachments", 5);
      when(result.listOf(CdsData.class)).thenReturn(parentData);

      assertThatThrownBy(() -> cut.validateForDraftSave(entity, statement, service))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void minNotMet_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      List<CdsData> parentData = createParentDataWithComposition("minOnlyAttachments", 1);
      when(result.listOf(CdsData.class)).thenReturn(parentData);

      assertThatThrownBy(() -> cut.validateForDraftSave(entity, statement, service))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void withWhereClause_appliesFilter() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY).where(e -> e.get("ID").eq("123"));
      List<CdsData> parentData = createValidParentData();
      when(result.listOf(CdsData.class)).thenReturn(parentData);

      cut.validateForDraftSave(entity, statement, service);

      ArgumentCaptor<CqnSelect> selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
      verify(service).run(selectCaptor.capture());
      CqnSelect capturedSelect = selectCaptor.getValue();
      assertThat(capturedSelect.where()).isPresent();
    }

    @Test
    void multipleParentEntities_aggregatesCount() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      // Two parents, each with 2 attachments = 4 total, exceeds max of 3
      CdsData parent1 = CdsData.create();
      parent1.put("maxOnlyAttachments", List.of(Map.of("ID", "att-1"), Map.of("ID", "att-2")));
      CdsData parent2 = CdsData.create();
      parent2.put("maxOnlyAttachments", List.of(Map.of("ID", "att-3"), Map.of("ID", "att-4")));
      when(result.listOf(CdsData.class)).thenReturn(List.of(parent1, parent2));

      assertThatThrownBy(() -> cut.validateForDraftSave(entity, statement, service))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void compositionDataNull_countsAsZero() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      CdsData parent = CdsData.create();
      parent.put("maxOnlyAttachments", null);
      parent.put("minOnlyAttachments", null);
      parent.put("minMaxAttachments", null);
      when(result.listOf(CdsData.class)).thenReturn(List.of(parent));

      // minOnlyAttachments requires min 2, null counts as 0
      assertThatThrownBy(() -> cut.validateForDraftSave(entity, statement, service))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }

    @Test
    void compositionDataNotList_countsAsZero() {
      CdsEntity entity = getCountValidatedEntity();
      Select<?> statement = Select.from(COUNT_VALIDATED_ENTITY);
      CdsData parent = CdsData.create();
      // Put a non-List value (String) for the composition
      parent.put("maxOnlyAttachments", "not a list");
      parent.put("minOnlyAttachments", "not a list");
      parent.put("minMaxAttachments", "not a list");
      when(result.listOf(CdsData.class)).thenReturn(List.of(parent));

      // minOnlyAttachments requires min 2, non-List counts as 0
      assertThatThrownBy(() -> cut.validateForDraftSave(entity, statement, service))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }
  }

  @Nested
  class CountAttachmentsInRequestData {

    @Test
    void compositionDataNotList_countsAsZero() {
      CdsEntity entity = getCountValidatedEntity();
      CdsData data = CdsData.create();
      data.put("ID", "entity-1");
      // Put a non-List value for the composition
      data.put("maxOnlyAttachments", "not a list");

      // Should not throw because non-List counts as 0, which is under max of 3
      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, List.of(data)));
    }

    @Test
    void compositionDataNull_countsAsZero() {
      CdsEntity entity = getCountValidatedEntity();
      CdsData data = CdsData.create();
      data.put("ID", "entity-1");
      data.put("maxOnlyAttachments", null);

      // Should not throw because null counts as 0, which is under max of 3
      assertThatNoException().isThrownBy(() -> cut.validateForCreate(entity, List.of(data)));
    }
  }
}
