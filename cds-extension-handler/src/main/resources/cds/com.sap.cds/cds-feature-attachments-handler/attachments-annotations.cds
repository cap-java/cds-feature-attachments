using {
    com.sap.attachments.MediaData,
    com.sap.attachments.Attachments
} from './attachments';

annotate MediaData with @UI.MediaResource: {
    Stream: content
} {
    content  @(
        title                      : '{i18n>content}',
        Core.MediaType             : mimeType,
        ContentDisposition.Filename: filename,
        Core.Immutable,
        odata.draft.skip
    );
    mimeType @(
        title: '{i18n>mimeType}',
        Core.IsMediaType,
        Core.Immutable
    );
}

annotate Attachments with @UI: {LineItem: [
    {Value: content},
    {Value: createdAt},
    {Value: createdBy},
    {Value: note}
]} {
    filename   @(title: '{i18n>fileName}');
    note       @(title: '{i18n>note}');
    documentId @(
        _is_document_id,
        readonly
    );
}
