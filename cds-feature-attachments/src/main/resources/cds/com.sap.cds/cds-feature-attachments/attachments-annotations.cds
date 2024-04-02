using {
    com.sap.attachments.MediaData,
    com.sap.attachments.Attachments
} from './attachments';

annotate MediaData with @UI.MediaResource: {Stream: content} {
    content    @(
        title                      : '{i18n>content}',
        Core.MediaType             : mimeType,
        ContentDisposition.Filename: fileName,
        ContentDisposition.Type    : 'inline'
    );
    mimeType   @(
        title: '{i18n>mimeType}',
        Core.IsMediaType
    );
    status @(UI.Hidden: true);
    documentId @(UI.Hidden: true);
}

annotate Attachments with @UI: {
    HeaderInfo: {
        $Type         : 'UI.HeaderInfoType',
        TypeName      : '{i18n>Attachment}',
        TypeNamePlural: '{i18n>Attachments}',
    },
    LineItem  : [
        {Value: content},
        {Value: createdAt},
        {Value: createdBy},
        {Value: note}
    ]
} {
    fileName @(title: '{i18n>fileName}');
    note     @(title: '{i18n>note}')
}
