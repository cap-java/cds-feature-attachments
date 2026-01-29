/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
