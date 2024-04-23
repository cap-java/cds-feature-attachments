using {
    com.sap.attachments.MediaData,
    com.sap.attachments.Attachments,
    com.sap.attachments.Statuses
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
        {
            $Type : 'UI.DataField',
            Value : status.text,
            Label : '{i18n>attachment_status}'
        },
        {Value: createdAt},
        {Value: createdBy},
        {Value: note}
    ]
} {
    note     @(title: '{i18n>attachment_note}');
    url     @(title: '{i18n>attachment_url}');
}

annotate Attachments with @Common: {
    SideEffects #ContentChanged : {
         SourceProperties : [
              content
         ],
         TargetProperties : [
              'status',
              'status.text',
              'status.code'
         ]
    }
}{};

annotate Statuses with{
    code @(
        Common.Text: {
            $value: ![text],
            ![@UI.TextArrangement]: #TextOnly
        }
    );
};

