using { sap.capire.bookshop as my } from '../db/schema';
using { sap.attachments.Attachments } from 'com.sap.cds/cds-feature-attachments';

extend my.Books with {
    attachments: Composition of many Attachments;
}

using { CatalogService as service } from './cat-service';
annotate service.Books with @(
  UI.Facets: [
    {
      $Type  : 'UI.ReferenceFacet',
      ID     : 'AttachmentsFacet',
      Label  : '{i18n>attachments}',
      Target : 'attachments/@UI.LineItem'
    }
  ]
);