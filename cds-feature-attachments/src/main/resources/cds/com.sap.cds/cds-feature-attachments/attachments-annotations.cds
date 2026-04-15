using {
    sap.attachments.Attachment,
    sap.attachments.MediaData,
    sap.attachments.Attachments
} from './attachments';

// Annotate Attachment type with a static Core.MediaType so that LargeBinary content is exposed as Edm.Stream (enabling Fiori upload widget).
// Using 'mimeType' (path reference) instead of a static value would break inline usage:
// CDS flattening rewrites 'content' to 'prefix_content' but does NOT rewrite the path reference 'mimeType' to 'prefix_mimeType', causing a broken reference to a non-existent field.
// Static annotations on the Attachment type propagate through CDS flattening to inline fields (e.g. profileIcon_status gets @readonly automatically).
// Only static values work here. Path references (like Core.MediaType: mimeType, ContentDisposition.Filename: fileName, or SideEffects) do NOT work for inline attachments due to CDS flattening limitations.

annotate sap.attachments.MediaData with @UI.MediaResource: {Stream: content} {
    content   @(
        title                           : '{i18n>attachment_content}',
        Core.ContentDisposition.Type    : 'inline',
        Core.MediaType                  : 'application/octet-stream'
    );
    mimeType  @(
        title: '{i18n>attachment_mimeType}'
    );
    fileName  @(
        title: '{i18n>attachment_fileName}',
        UI.MultiLineText,
        readonly
        );
    status    @(title: '{i18n>attachment_status}', readonly);
    contentId @(UI.Hidden: true, readonly);
    scannedAt @(UI.Hidden: true, readonly);
}

annotate sap.attachments.Attachments with
@Capabilities: {
    UpdateRestrictions.NonUpdateableProperties: [content],
    SortRestrictions: { NonSortableProperties: [content] }
}
@UI: {
    HeaderInfo: {
        TypeName      : '{i18n>attachment}',
        TypeNamePlural: '{i18n>attachments}',
    },
    LineItem  : [
        {Value: content,   @HTML5.CssDefaults: {width: '30%'}},
        {Value: status, Criticality: statusNav.criticality,    @HTML5.CssDefaults: {width: '10%'}},
        {Value: createdAt, @HTML5.CssDefaults: {width: '20%'}},
        {Value: createdBy, @HTML5.CssDefaults: {width: '15%'}},
        {Value: note,      @HTML5.CssDefaults: {width: '25%'}},
        {Value: up__ID, @UI.Hidden}
    ]
}
@Common: {SideEffects #ContentChanged: {
    SourceProperties: [content],
    TargetProperties: ['status']
}} {
    content    @(
        Core.ContentDisposition.Filename: fileName,
        Core.MediaType: mimeType
    );
    mimeType   @Core.IsMediaType;
    status     @(Common.Text : statusNav.name, Common.TextArrangement : #TextOnly);
    note       @(
        title: '{i18n>attachment_note}',
        UI.MultiLineText
    );
    modifiedAt @(odata.etag);
}

annotate sap.attachments.Attachment with {
    content @Core.ContentDisposition.Filename: fileName;
}
