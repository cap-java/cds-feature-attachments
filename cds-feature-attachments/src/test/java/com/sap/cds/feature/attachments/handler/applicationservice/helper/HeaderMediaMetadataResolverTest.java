/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeaderMediaMetadataResolverTest {

  private static final String MEDIA_ENTITY = "unit.test.Attachment";

  private static CdsRuntime runtime;

  private EventContext eventContext;
  private ParameterInfo parameterInfo;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    eventContext = mock(EventContext.class);
    parameterInfo = mock(ParameterInfo.class);
    when(eventContext.getParameterInfo()).thenReturn(parameterInfo);
  }

  @Test
  void extractsFileNameFromContentDispositionHeader() {
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"report.pdf\"");

    assertThat(HeaderMediaMetadataResolver.extractFileNameFromHeader(eventContext))
        .contains("report.pdf");
  }

  @Test
  void extractsFileNameFromSlugHeader() {
    when(parameterInfo.getHeader("Content-Disposition")).thenReturn(null);
    when(parameterInfo.getHeader("slug")).thenReturn("document.docx");

    assertThat(HeaderMediaMetadataResolver.extractFileNameFromHeader(eventContext))
        .contains("document.docx");
  }

  @Test
  void extractsMimeTypeFromContentTypeHeaderStrippingParameters() {
    when(parameterInfo.getHeader("Content-Type")).thenReturn("text/html; charset=utf-8");

    assertThat(HeaderMediaMetadataResolver.extractMimeTypeFromHeader(eventContext))
        .contains("text/html");
  }

  @Test
  void returnsEmptyWhenNoHeadersPresent() {
    assertThat(HeaderMediaMetadataResolver.extractFileNameFromHeader(eventContext)).isEmpty();
    assertThat(HeaderMediaMetadataResolver.extractMimeTypeFromHeader(eventContext)).isEmpty();
  }

  @Test
  void fillsFileNameAndMimeTypeFromHeadersWhenAbsentInData() {
    CdsEntity entity = runtime.getCdsModel().getEntity(MEDIA_ENTITY);
    Attachments data = Attachments.create();
    data.setContent(mock(InputStream.class));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"notes.txt\"");
    when(parameterInfo.getHeader("Content-Type")).thenReturn("text/html");

    HeaderMediaMetadataResolver.applyHeaderFallback(entity, List.of(data), eventContext);

    assertThat(data.getFileName()).isEqualTo("notes.txt");
    assertThat(data.getMimeType()).isEqualTo("text/html");
  }

  @Test
  void doesNotOverridePayloadValuesWithHeaderValues() {
    CdsEntity entity = runtime.getCdsModel().getEntity(MEDIA_ENTITY);
    Attachments data = Attachments.create();
    data.setContent(mock(InputStream.class));
    data.setFileName("payload.png");
    data.setMimeType("image/png");
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"notes.txt\"");
    when(parameterInfo.getHeader("Content-Type")).thenReturn("text/html");

    HeaderMediaMetadataResolver.applyHeaderFallback(entity, List.of(data), eventContext);

    assertThat(data.getFileName()).isEqualTo("payload.png");
    assertThat(data.getMimeType()).isEqualTo("image/png");
  }

  @Test
  void doesNothingWhenDataHasNoContentElement() {
    CdsEntity entity = runtime.getCdsModel().getEntity(MEDIA_ENTITY);
    Attachments data = Attachments.create();
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"notes.txt\"");

    HeaderMediaMetadataResolver.applyHeaderFallback(entity, List.of(data), eventContext);

    assertThat(data.getFileName()).isNull();
    assertThat(data.getMimeType()).isNull();
  }
}
