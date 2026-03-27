using {sap.capire.bookshop as my} from '../db/schema';
using {sap.attachments.Attachments} from 'com.sap.cds/cds-feature-attachments';
using {sap.attachments.Attachment} from 'com.sap.cds/cds-feature-attachments';

// Extend Books entity to support file attachments (images, PDFs, documents)
// Each book can have multiple attachments via composition relationship
extend my.Books with {
  attachments                      : Composition of many Attachments;
  @UI.Hidden
  sizeLimitedAttachments           : Composition of many Attachments;
  @UI.Hidden
  mediaValidatedAttachments        : Composition of many Attachments;
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

// Extend Books entity with an inline single-file attachment (profile icon)
extend my.Books with {
  profileIcon : Attachment;
}

// Add UI component for attachments table to the Browse Books App
using {CatalogService as service} from '../app/services';

annotate service.Books with @(UI.Facets: [{
  $Type : 'UI.ReferenceFacet',
  ID    : 'AttachmentsFacet',
  Label : '{i18n>attachments}',
  Target: 'attachments/@UI.LineItem'
}]);

// AdminService Facets (including attachments and profileIcon) are defined in
// app/admin-books/fiori-service.cds. Don't re-annotate UI.Facets here,
// as it would override the complete facet list defined there.


service nonDraft {
  entity Books as projection on my.Books;
}
