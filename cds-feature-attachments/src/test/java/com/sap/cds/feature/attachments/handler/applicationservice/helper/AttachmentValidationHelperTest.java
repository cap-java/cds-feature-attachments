/*
* © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
*/
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

class AttachmentValidationHelperTest {

    // ---------- constructor ----------

    @Test
    void constructorShouldBeCallableForCoverage() {
        new AttachmentValidationHelper();
    }

    // ---------- validateMimeTypeForAttachment ----------
    @Test
    void shouldAcceptMimeTypeFromURLConnection() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "document.pdf",
                List.of("application/pdf"));
        assertEquals("application/pdf", result);
    }

    @Test
    void shouldUseExtensionMapWhenURLConnectionDoesNotDetectMimeType() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "image.webp",
                List.of("image/webp"));
        assertEquals("image/webp", result);
    }

    @Test
    void shouldSupportWildCardTypes() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "image.jpeg",
                List.of("image/*"));
        assertEquals("image/jpeg", result);
    }

    @Test
    void shouldAllowAllMimeTypesWithStarSlashStar() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "anyfile.jpeg",
                List.of("*/*"));
        assertEquals("image/jpeg", result);
    }

    @Test
    void shouldAllowRandomInvalidExtensionWithStarSlashStar() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "anyfile.anyext",
                List.of("*/*"));
        assertEquals(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE, result);
    }

    @Test
    void shouldAllowRandomInvalidExtensionWithNullAcceptableMediaTypes() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "anyfile.anyext",
                null);
        assertEquals(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE, result);
    }

    @Test
    void shouldNotAllowRandomInvalidExtensionWithStarSlashStar() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> AttachmentValidationHelper.validateMediaTypeForAttachment(
                        "anyfile.anyext",
                        List.of("application/pdf")));

        assertTrue(ex.getErrorStatus().equals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void shouldAllowAllMimeTypesIfNoRestrictionsGiven() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "anyfile.anyext",
                List.of());
        assertEquals(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE, result);
    }

    @Test
    void shouldUseDefaultMimeTypeWhenExtensionIsUnknown() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "file.unknownext",
                List.of(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE));
        assertEquals(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE, result);
    }

    @Test
    void shouldThrowExceptionWhenInvalidFileName() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> AttachmentValidationHelper.validateMediaTypeForAttachment(
                        "invalidfilename",
                        List.of("application/pdf")));

        assertTrue(ex.getErrorStatus().equals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void shouldThrowExceptionWhenFilenameEndsWithDot() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> AttachmentValidationHelper.validateMediaTypeForAttachment(
                        "file.",
                        List.of("application/pdf")));

        assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
    }

    @Test
    void shouldHandleUppercaseExtension() {
        String result = AttachmentValidationHelper.validateMediaTypeForAttachment(
                "photo.JPG",
                List.of("image/jpeg"));

        assertEquals("image/jpeg", result);
    }

    @Test
    void shouldThrowExceptionWhenMimeTypeNotAllowed() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> AttachmentValidationHelper.validateMediaTypeForAttachment(
                        "document.pdf",
                        List.of("image/png")));

        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void shouldThrowExceptionWhenDefaultMimeTypeNotAllowed() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> AttachmentValidationHelper.validateMediaTypeForAttachment(
                        "file.unknownext",
                        List.of("application/pdf")));

        assertTrue(ex.getMessage().contains("not allowed"));
    }

}
