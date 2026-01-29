/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AttachmentCountValidationHandlerTest {

  private AttachmentCountValidationHandler cut;
  private AttachmentCountValidator validator;

  @BeforeEach
  void setup() {
    validator = new AttachmentCountValidator();
    cut = new AttachmentCountValidationHandler(validator);
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
  class ValidateOnCreate {

    @Test
    void withinMaxLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsCreateEventContext context = mock(CdsCreateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateOnCreate(context, data));
    }

    @Test
    void exceedsMaxLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsCreateEventContext context = mock(CdsCreateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("maxOnlyAttachments", 4);

      assertThatThrownBy(() -> cut.validateOnCreate(context, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void minMaxAttachments_atMinLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsCreateEventContext context = mock(CdsCreateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateOnCreate(context, data));
    }

    @Test
    void minMaxAttachments_atMaxLimit_noException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsCreateEventContext context = mock(CdsCreateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 3);

      assertThatNoException().isThrownBy(() -> cut.validateOnCreate(context, data));
    }
  }

  @Nested
  class ValidateOnUpdate {

    @Test
    void withinLimits_noException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsUpdateEventContext context = mock(CdsUpdateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 2);

      assertThatNoException().isThrownBy(() -> cut.validateOnUpdate(context, data));
    }

    @Test
    void exceedsMaxLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsUpdateEventContext context = mock(CdsUpdateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 4);

      assertThatThrownBy(() -> cut.validateOnUpdate(context, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MaximumAmountExceeded");
    }

    @Test
    void belowMinLimit_throwsException() {
      CdsEntity entity = getCountValidatedEntity();
      CdsUpdateEventContext context = mock(CdsUpdateEventContext.class);
      when(context.getTarget()).thenReturn(entity);

      List<CdsData> data = createDataWithAttachments("minMaxAttachments", 1);

      assertThatThrownBy(() -> cut.validateOnUpdate(context, data))
          .isInstanceOf(ServiceException.class)
          .hasMessageContaining("MinimumAmountNotFulfilled");
    }
  }
}
