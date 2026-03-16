/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.services.messages.LocalizedMessageProvider;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentsLocalizedMessageProviderTest {

  private AttachmentsLocalizedMessageProvider cut;
  private LocalizedMessageProvider previousProvider;

  @BeforeEach
  void setup() {
    cut = new AttachmentsLocalizedMessageProvider();
    previousProvider = mock(LocalizedMessageProvider.class);
  }

  @Test
  void knownKeyReturnsFormattedMessage() {
    var result = cut.get(MessageKeys.FILE_SIZE_EXCEEDED, new Object[] {"400MB"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("File size exceeds the limit of 400MB.");
  }

  @Test
  void knownKeyWithoutArgsReturnsMessage() {
    var result = cut.get(MessageKeys.FILE_SIZE_EXCEEDED_NO_SIZE, null, Locale.ENGLISH);

    assertThat(result).isEqualTo("File size exceeds the limit.");
  }

  @Test
  void knownKeyWithEmptyArgsReturnsMessage() {
    var result = cut.get(MessageKeys.INVALID_CONTENT_LENGTH, new Object[] {}, Locale.ENGLISH);

    assertThat(result).isEqualTo("Invalid Content-Length header.");
  }

  @Test
  void unknownKeyDelegatesToPreviousProvider() {
    cut.setPrevious(previousProvider);
    var args = new Object[] {"arg1"};
    when(previousProvider.get("unknown.key", args, Locale.ENGLISH)).thenReturn("previous result");

    var result = cut.get("unknown.key", args, Locale.ENGLISH);

    assertThat(result).isEqualTo("previous result");
    verify(previousProvider).get("unknown.key", args, Locale.ENGLISH);
  }

  @Test
  void unknownKeyWithNoPreviousReturnsNull() {
    var result = cut.get("unknown.key", new Object[] {}, Locale.ENGLISH);

    assertThat(result).isNull();
  }

  @Test
  void knownKeyDoesNotDelegateToPrevious() {
    cut.setPrevious(previousProvider);

    var result = cut.get(MessageKeys.FILE_SIZE_EXCEEDED, new Object[] {"10KB"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("File size exceeds the limit of 10KB.");
    verifyNoInteractions(previousProvider);
  }

  @Test
  void nullLocaleUsesDefault() {
    var result = cut.get(MessageKeys.FILE_SIZE_EXCEEDED, new Object[] {"100MB"}, null);

    assertThat(result).isNotNull();
    assertThat(result).contains("100MB");
  }

  @Test
  void setPreviousStoresProvider() {
    cut.setPrevious(previousProvider);
    when(previousProvider.get("any.key", null, Locale.ENGLISH)).thenReturn("from previous");

    var result = cut.get("any.key", null, Locale.ENGLISH);

    assertThat(result).isEqualTo("from previous");
  }

  @Test
  void uploadFailedKeyReturnsFormattedMessage() {
    var result = cut.get(MessageKeys.UPLOAD_FAILED, new Object[] {"test.pdf"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("Failed to upload file test.pdf.");
  }

  @Test
  void deleteFailedKeyReturnsFormattedMessage() {
    var result = cut.get(MessageKeys.DELETE_FAILED, new Object[] {"doc-123"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("Failed to delete file with document id doc-123.");
  }

  @Test
  void readFailedKeyReturnsFormattedMessage() {
    var result = cut.get(MessageKeys.READ_FAILED, new Object[] {"doc-456"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("Failed to read file with document id doc-456.");
  }

  @Test
  void documentNotFoundKeyReturnsFormattedMessage() {
    var result = cut.get(MessageKeys.DOCUMENT_NOT_FOUND, new Object[] {"doc-789"}, Locale.ENGLISH);

    assertThat(result).isEqualTo("Document not found for id doc-789.");
  }
}
