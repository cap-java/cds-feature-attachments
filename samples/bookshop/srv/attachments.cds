using {sap.capire.bookshop as my} from '../db/schema';
using {Attachments} from 'com.sap.cds/cds-feature-attachments';
using {Attachment} from 'com.sap.cds/cds-feature-attachments';

// Extend Books entity to support file attachments (images, PDFs, documents)
// Each book can have multiple attachments via composition relationship
extend my.Books with {
  attachments               : Composition of many Attachments;
  @UI.Hidden
  sizeLimitedAttachments    : Composition of many Attachments;
  @UI.Hidden
  mediaValidatedAttachments : Composition of many Attachments;
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

// Extend Books entity with inline single-file attachments
extend my.Books with {
  profileIcon : Attachment;
  coverImage  : Attachment;
}

annotate my.Books:profileIcon with {
  content @Validation.Maximum: '1MB'  @Core.AcceptableMediaTypes: ['image/*'];
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

annotate adminService.Books with @(UI.Facets: [
  {
    $Type : 'UI.ReferenceFacet',
    ID    : 'AttachmentsFacet',
    Label : '{i18n>attachments}',
    Target: 'attachments/@UI.LineItem'
  },
  {
    $Type : 'UI.ReferenceFacet',
    Label : 'Profile Icon',
    Target: '@UI.FieldGroup#ProfileIcon'
  },
  {
    $Type : 'UI.ReferenceFacet',
    Label : 'Cover Image',
    Target: '@UI.FieldGroup#CoverImage'
  }
]);

annotate adminService.Books with @(UI: {
  FieldGroup #ProfileIcon: {Data: [
    {Value: profileIcon_content, Label: 'Download'},
    {Value: profileIcon_fileName},
    {Value: profileIcon_status},
    {Value: profileIcon_note}
  ]},
  FieldGroup #CoverImage: {Data: [
    {Value: coverImage_content},
    {Value: coverImage_fileName},
    {Value: coverImage_status},
    {Value: coverImage_note}
  ]}
});

