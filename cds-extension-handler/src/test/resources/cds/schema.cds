namespace unit.test;

using { cuid } from '@sap/cds/common';
using { com.sap.attachments.Attachments } from '../../../main/resources/cds/com.sap.cds/cds-feature-attachments-handler';

entity Attachment : Attachments {
    parentKey : UUID;
}

entity Roots: cuid {
        title       : String;
        itemTable   : Composition of many Items
                          on itemTable.rootId = $self.ID;
        attachments : Composition of many Attachment
                          on attachments.parentKey = $self.ID;
}

entity Items: cuid {
        rootId      : UUID;
        note        : String;
        attachments : Composition of many Attachment
                          on attachments.parentKey = $self.ID;
}

entity wrongAttachment@(_is_media_data) : cuid  {
      url      : String;
      content  : LargeBinary @Core.MediaType; // only for db-based services
      mimeType : String;
      filename : String;
}

service TestService{
    entity RootTable as projection on Roots;
}
