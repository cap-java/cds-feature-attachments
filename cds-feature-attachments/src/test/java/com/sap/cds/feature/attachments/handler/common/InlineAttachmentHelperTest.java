/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnlyTable_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InlineAttachmentHelperTest {

  private static CdsRuntime runtime;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @Test
  void findInlineAttachmentPrefixes_returnsPrefix_forInlineOnlyEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineAttachmentPrefixes(entity);

    assertThat(result).containsExactly("avatar");
  }

  @Test
  void findInlineAttachmentPrefixes_returnsPrefix_forRootTableWithInline() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineAttachmentPrefixes(entity);

    assertThat(result).containsExactly("profilePicture");
  }

  @Test
  void findInlineAttachmentPrefixes_returnsEmpty_forMediaEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineAttachmentPrefixes(entity);

    assertThat(result).isEmpty();
  }

  @Test
  void findInlineAttachmentPrefixes_returnsEmpty_forEntityWithoutInline() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineAttachmentPrefixes(entity);

    assertThat(result).isEmpty();
  }

  @Test
  void hasInlineAttachments_returnsTrue_forInlineOnlyEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    assertThat(InlineAttachmentHelper.hasInlineAttachments(entity)).isTrue();
  }

  @Test
  void hasInlineAttachments_returnsTrue_forRootTableWithInline() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();

    assertThat(InlineAttachmentHelper.hasInlineAttachments(entity)).isTrue();
  }

  @Test
  void hasInlineAttachments_returnsFalse_forMediaEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    assertThat(InlineAttachmentHelper.hasInlineAttachments(entity)).isFalse();
  }

  @Test
  void hasInlineAttachments_returnsFalse_forEntityWithoutInline() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();

    assertThat(InlineAttachmentHelper.hasInlineAttachments(entity)).isFalse();
  }

  @Test
  void buildInlineFieldName_combinesPrefixAndField() {
    assertThat(InlineAttachmentHelper.buildInlineFieldName("avatar", "contentId"))
        .isEqualTo("avatar_contentId");
  }

  @Test
  void buildInlineFieldName_combinesLongerPrefix() {
    assertThat(InlineAttachmentHelper.buildInlineFieldName("profilePicture", "status"))
        .isEqualTo("profilePicture_status");
  }

  @Test
  void extractPrefix_removesContentSuffix() {
    assertThat(InlineAttachmentHelper.extractPrefix("avatar_content")).isEqualTo("avatar");
  }

  @Test
  void extractPrefix_handlesLongerPrefix() {
    assertThat(InlineAttachmentHelper.extractPrefix("profilePicture_content"))
        .isEqualTo("profilePicture");
  }

  @Test
  void findInlineContentElements_returnsContentFields_forInlineOnlyEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineContentElements(entity);

    assertThat(result).containsExactly("avatar_content");
  }

  @Test
  void findInlineContentElements_returnsContentFields_forRootTable() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineContentElements(entity);

    assertThat(result).containsExactly("profilePicture_content");
  }

  @Test
  void findInlineContentElements_returnsEmpty_forMediaEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineContentElements(entity);

    assertThat(result).isEmpty();
  }

  @Test
  void findInlineContentElements_returnsEmpty_forEntityWithoutInline() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();

    var result = InlineAttachmentHelper.findInlineContentElements(entity);

    assertThat(result).isEmpty();
  }

  @Test
  void readInlineAttachmentState_returnsEmptyForEmptyPrefixes() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME);

    var result =
        InlineAttachmentHelper.readInlineAttachmentState(persistence, entity, statement, List.of());

    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void readInlineAttachmentState_readsAndMapsFields() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var id = UUID.randomUUID().toString();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME).byId(id);

    Row row =
        createRow(
            "ID",
            id,
            "avatar_contentId",
            "doc-123",
            "avatar_status",
            "Clean",
            "avatar_scannedAt",
            Instant.parse("2025-01-01T00:00:00Z"));

    Result result = mock(Result.class);
    doAnswer(
            invocation -> {
              Consumer<Row> consumer = invocation.getArgument(0);
              List.of(row).forEach(consumer);
              return null;
            })
        .when(result)
        .forEach(any(Consumer.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    var attachments =
        InlineAttachmentHelper.readInlineAttachmentState(
            persistence, entity, statement, List.of("avatar"));

    assertThat(attachments).hasSize(1);
    var att = attachments.get(0);
    assertThat(att.getContentId()).isEqualTo("doc-123");
    assertThat(att.getStatus()).isEqualTo("Clean");
    assertThat(att.getScannedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    assertThat(att.get("ID")).isEqualTo(id);

    ArgumentCaptor<CqnSelect> selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
    verify(persistence).run(selectCaptor.capture());
    var select = selectCaptor.getValue();
    assertThat(select.toString()).contains("avatar_contentId");
    assertThat(select.toString()).contains("avatar_status");
    assertThat(select.toString()).contains("avatar_scannedAt");
  }

  @SuppressWarnings("unchecked")
  @Test
  void readInlineAttachmentState_handlesNullScannedAt() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME);

    Row row =
        createRow(
            "ID",
            "id-1",
            "avatar_contentId",
            "doc-456",
            "avatar_status",
            "Unscanned",
            "avatar_scannedAt",
            null);

    Result result = mock(Result.class);
    doAnswer(
            invocation -> {
              Consumer<Row> consumer = invocation.getArgument(0);
              List.of(row).forEach(consumer);
              return null;
            })
        .when(result)
        .forEach(any(Consumer.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    var attachments =
        InlineAttachmentHelper.readInlineAttachmentState(
            persistence, entity, statement, List.of("avatar"));

    assertThat(attachments).hasSize(1);
    assertThat(attachments.get(0).getScannedAt()).isNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  void readInlineAttachmentState_handlesNonInstantScannedAt() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME);

    Row row =
        createRow(
            "ID",
            "id-1",
            "avatar_contentId",
            "doc-789",
            "avatar_status",
            "Clean",
            "avatar_scannedAt",
            "not-an-instant");

    Result result = mock(Result.class);
    doAnswer(
            invocation -> {
              Consumer<Row> consumer = invocation.getArgument(0);
              List.of(row).forEach(consumer);
              return null;
            })
        .when(result)
        .forEach(any(Consumer.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    var attachments =
        InlineAttachmentHelper.readInlineAttachmentState(
            persistence, entity, statement, List.of("avatar"));

    assertThat(attachments).hasSize(1);
    assertThat(attachments.get(0).getScannedAt()).isNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  void readInlineAttachmentState_multipleRows() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME);

    Row row1 =
        createRow(
            "ID",
            "id-1",
            "avatar_contentId",
            "doc-1",
            "avatar_status",
            "Clean",
            "avatar_scannedAt",
            Instant.now());
    Row row2 =
        createRow(
            "ID",
            "id-2",
            "avatar_contentId",
            "doc-2",
            "avatar_status",
            "Infected",
            "avatar_scannedAt",
            Instant.now());

    Result result = mock(Result.class);
    doAnswer(
            invocation -> {
              Consumer<Row> consumer = invocation.getArgument(0);
              List.of(row1, row2).forEach(consumer);
              return null;
            })
        .when(result)
        .forEach(any(Consumer.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    var attachments =
        InlineAttachmentHelper.readInlineAttachmentState(
            persistence, entity, statement, List.of("avatar"));

    assertThat(attachments).hasSize(2);
    assertThat(attachments.get(0).getContentId()).isEqualTo("doc-1");
    assertThat(attachments.get(1).getContentId()).isEqualTo("doc-2");
  }

  @SuppressWarnings("unchecked")
  @Test
  void readInlineAttachmentState_withWhereClause() {
    PersistenceService persistence = mock(PersistenceService.class);
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();
    var id = UUID.randomUUID().toString();
    var statement = Delete.from(InlineOnlyTable_.CDS_NAME).byId(id);

    Result result = mock(Result.class);
    doAnswer(
            invocation -> {
              Consumer<Row> consumer = invocation.getArgument(0);
              List.<Row>of().forEach(consumer);
              return null;
            })
        .when(result)
        .forEach(any(Consumer.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    var attachments =
        InlineAttachmentHelper.readInlineAttachmentState(
            persistence, entity, statement, List.of("avatar"));

    assertThat(attachments).isEmpty();

    ArgumentCaptor<CqnSelect> selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
    verify(persistence).run(selectCaptor.capture());
    var select = selectCaptor.getValue();
    assertThat(select.toString()).contains(id);
  }

  private static Row createRow(Object... keyValues) {
    java.util.Map<String, Object> map = new java.util.HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return Struct.access(map).as(Row.class);
  }
}
