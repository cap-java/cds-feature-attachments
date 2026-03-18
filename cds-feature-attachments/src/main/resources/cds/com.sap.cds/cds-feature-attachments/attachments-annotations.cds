using {
    sap.attachments.MediaData,
    sap.attachments.Attachments
} from './attachments';

annotate MediaData with @UI.MediaResource: {Stream: content} {
    content    @(
        title                           : '{i18n>attachment_content}',
        Core.MediaType                  : mimeType,
        Core.ContentDisposition.Filename: filename,
        Core.ContentDisposition.Type    : 'inline'
    )                                                @odata.draft.skip;
    mimeType   @(
        title: '{i18n>attachment_mimeType}',
        Core.IsMediaType
    );
    filename   @(
        title: '{i18n>attachment_filename}',
        UI.MultiLineText
    );
    url        @(UI.Hidden);
    hash       @(UI.Hidden)                          @Core.Computed;
    status     @(title: '{i18n>attachment_status}')  @readonly;
    contentId  @(UI.Hidden)                          @readonly;
    lastScan   @(UI.Hidden)                          @Core.Computed;
}

annotate Attachments with  @UI: {
    HeaderInfo: {
        $Type         : 'UI.HeaderInfoType',
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
}  @Capabilities: {SortRestrictions: {NonSortableProperties: [content]}}  {
    content
               @Core.ContentDisposition: {
        Filename: filename,
        Type    : 'inline'
    };
    note       @(
        title: '{i18n>attachment_note}',
        UI.MultiLineText
    );
    modifiedAt @(odata.etag);
}

annotate Attachments with @Common: {SideEffects #ContentChanged: {
    SourceProperties: [content],
    TargetProperties: ['status']
}} {};
