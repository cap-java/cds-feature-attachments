using {
    com.sap.attachments.MediaData,
    com.sap.attachments.Attachments
} from './attachments';

annotate MediaData with @UI.MediaResource: {Stream: content} {
    content    @(
        title                      : '{i18n>attachment_content}',
        Core.MediaType             : mimeType,
        ContentDisposition.Filename: fileName,
        ContentDisposition.Type    : 'inline'
    );
    mimeType   @(
        title: '{i18n>attachment_mimeType}',
        Core.IsMediaType
    );
    fileName @(title: '{i18n>attachment_fileName}');
    status @(
        Common.Label: '{@i18n>attachment_status}',
        Common.Text: {
            $value: ![status.text],
            ![@UI.TextArrangement]: #TextOnly
        },
        ValueList: {entity:'Statuses'},
        sap.value.list: 'fixed-values'
    );
    documentId @(UI.Hidden: true);
    scannedAt @(UI.Hidden: true);
}

annotate Attachments with @UI: {
    HeaderInfo: {
        $Type         : 'UI.HeaderInfoType',
        TypeName      : '{i18n>attachment}',
        TypeNamePlural: '{i18n>attachments}',
    },
    LineItem  : [
        {Value: content},
        {Value: status},
        {Value: createdAt},
        {Value: createdBy},
        {Value: note}
    ]
} {
    note     @(title: '{i18n>attachment_note}');
    url     @(title: '{i18n>attachment_url}');
}
