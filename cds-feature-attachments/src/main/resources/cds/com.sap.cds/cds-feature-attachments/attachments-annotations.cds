using {
    sap.attachments.MediaData,
    sap.attachments.Attachments
} from './attachments';

annotate sap.attachments.MediaData with @UI.MediaResource: {Stream: content} {
    content   @(
        title         : '{i18n>attachment_content}',
        Core.MediaType: mimeType,
    );
    mimeType  @(
        title: '{i18n>attachment_mimeType}',
        Core.IsMediaType
    );
    fileName  @(
        title: '{i18n>attachment_fileName}',
        UI.MultiLineText
    );
    status    @(
        title: '{i18n>attachment_status}',
        readonly
    );
    note      @(
        title: '{i18n>attachment_note}',
        UI.MultiLineText
    );
    contentId @(
        UI.Hidden: true,
        readonly
    );
    scannedAt @(
        UI.Hidden: true,
        readonly
    );
}

annotate sap.attachments.Attachments {
    content    @(
        Core.ContentDisposition: {
            Filename: fileName,
            Type    : 'inline',
        },
        Core.MediaType         : mimeType
    );
    mimeType   @Core.IsMediaType;
    status     @(
        Common.Text           : statusNav.name,
        Common.TextArrangement: #TextOnly
    );
    modifiedAt @(odata.etag);
}

// Fiori Annotations
annotate sap.attachments.Attachments with
@Capabilities: {
    UpdateRestrictions.NonUpdateableProperties: [content],
    SortRestrictions                          : {NonSortableProperties: [content]}
}
@UI          : {
    HeaderInfo: {
        TypeName      : '{i18n>attachment}',
        TypeNamePlural: '{i18n>attachments}',
    },
    LineItem  : [
        {
            Value             : content,
            @HTML5.CssDefaults: {width: '30%'}
        },
        {
            Value             : status,
            Criticality       : statusNav.criticality,
            @HTML5.CssDefaults: {width: '10%'}
        },
        {
            Value             : createdAt,
            @HTML5.CssDefaults: {width: '20%'}
        },
        {
            Value             : createdBy,
            @HTML5.CssDefaults: {width: '15%'}
        },
        {
            Value             : note,
            @HTML5.CssDefaults: {width: '25%'}
        },
        {
            Value: up__ID,
            @UI.Hidden
        }
    ]
}
@Common      : {SideEffects #ContentChanged: {
    SourceProperties: [content],
    TargetProperties: ['status']
}}
