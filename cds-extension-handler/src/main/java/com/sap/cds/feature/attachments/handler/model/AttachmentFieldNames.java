package com.sap.cds.feature.attachments.handler.model;

import java.util.Optional;

public record AttachmentFieldNames(String keyField, Optional<String> documentIdField, Optional<String> mimeTypeField,
                                   Optional<String> fileNameField, String contentFieldName) {
}
