using {
    sap.attachments.MediaData,
    sap.attachments.Attachments
} from './attachments';

annotate MediaData with @UI.MediaResource: {Stream: content} {
    content   @(
        title                           : '{i18n>attachment_content}',
        Core.MediaType                  : mimeType,
        Core.ContentDisposition.Filename: fileName,
        Core.ContentDisposition.Type    : 'inline'
    );
    mimeType  @(
        title: '{i18n>attachment_mimeType}',
        Core.IsMediaType
    );
    fileName  @(title: '{i18n>attachment_fileName}');
    status    @(title: '{i18n>attachment_status}');
    contentId @(UI.Hidden: true);
    scannedAt @(UI.Hidden: true);
}

annotate Attachments with @UI: {
    HeaderInfo: {
        $Type         : 'UI.HeaderInfoType',
        TypeName      : '{i18n>attachment}',
        TypeNamePlural: '{i18n>attachments}',
    },
    LineItem  : [
        {Value: content,   @HTML5.CssDefaults: {width: '30%'}},
        {Value: status,    @HTML5.CssDefaults: {width: '10%'}},
        {Value: createdAt, @HTML5.CssDefaults: {width: '20%'}},
        {Value: createdBy, @HTML5.CssDefaults: {width: '15%'}},
        {Value: note,      @HTML5.CssDefaults: {width: '25%'}},
        {Value: up__ID, @UI.Hidden}
    ]
} {
    note       @(title: '{i18n>attachment_note}');
    modifiedAt @(odata.etag);
}

annotate Attachments with @Common: {SideEffects #ContentChanged: {
    SourceProperties: [content],
    TargetProperties: ['status']
}} {};
