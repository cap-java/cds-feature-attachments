using {sap.capire.bookshop as my} from '../db/schema';
using {sap.attachments.Attachments} from 'com.sap.cds/cds-feature-attachments';

// Extend Books entity to support file attachments (images, PDFs, documents)
// Each book can have multiple attachments via composition relationship
extend my.Books with {
  attachments                      : Composition of many Attachments;
  @UI.Hidden
  sizeLimitedAttachments           : Composition of many Attachments;
  @UI.Hidden
  mediaValidatedAttachments        : Composition of one Attachments;
  @UI.Hidden
  anotherMediaValidatedAttachments : Composition of many Attachments;
}

annotate my.Books.sizeLimitedAttachments with {
  content @Validation.Maximum: '5MB';
}

// Media type validation for attachments
annotate my.Books.mediaValidatedAttachments with {
  content @Core.AcceptableMediaTypes: [
    'image/jpeg',
    'image/png'
  ];
}

// Add UI component for attachments table to the Browse Books App
using {CatalogService as service} from '../app/services';

annotate service.Books with @(UI.Facets: [{
  $Type : 'UI.ReferenceFacet',
  ID    : 'AttachmentsFacet',
  Label : '{i18n>attachments}',
  Target: 'attachments/@UI.LineItem'
}]);

// Adding the UI Component (a table) to the Administrator App
using {AdminService as adminService} from '../app/services';

annotate adminService.Books with @(UI.Facets: [{
  $Type : 'UI.ReferenceFacet',
  ID    : 'AttachmentsFacet',
  Label : '{i18n>attachments}',
  Target: 'mediaValidatedAttachments/@UI.FieldGroup'
}]);

annotate adminService.Books with @odata.draft.enabled;


annotate adminService.Books.mediaValidatedAttachments with @UI.FieldGroup: {
    Data: [
        {Value: content},
        {Value: fileName},
        {Value: mimeType},
        {Value: status}
    ]
};

service nonDraft {
  entity Books as projection on my.Books;
}
