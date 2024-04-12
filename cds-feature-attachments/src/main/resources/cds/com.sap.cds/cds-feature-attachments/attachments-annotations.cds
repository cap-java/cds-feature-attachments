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
    fileName @(title: '{i18n>fileName}');
    status_code @(readonly);
    status @(
        readonly,
        Common.Label: '{@i18n>status}',
        Common.Text: {
            $value: ![statusText],
            ![@UI.TextArrangement]: #TextOnly
        },
        ValueList: {entity:'Statuses'},
        sap.value.list: 'fixed-values'
    );
    documentId @(UI.Hidden: true);
    scannedAt @(UI.Hidden: true,
                readonly);
}

annotate Attachments with @UI: {
    HeaderInfo: {
        $Type         : 'UI.HeaderInfoType',
        TypeName      : '{i18n>Attachment}',
        TypeNamePlural: '{i18n>Attachments}',
    },
    LineItem  : [
        {Value: content},
        {Value: status_code},
        {Value: createdAt},
        {Value: createdBy},
        {Value: note}
    ]
} {
    note     @(title: '{i18n>note}');
}

annotate Statuses with{
    code @(
        Common.Text: {
            $value: ![statusText],
            ![@UI.TextArrangement]: #TextOnly
        }
    );
};
