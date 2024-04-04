namespace test.data.model;

using {cuid} from '@sap/cds/common';
using {com.sap.attachments.Attachments} from`com.sap.cds/cds-feature-attachments`;
using from '@sap/cds/srv/outbox';

entity AttachmentEntity : Attachments {
    parentKey : UUID;
}

entity Roots : cuid {
    title       : String;
    attachments : Composition of many AttachmentEntity
                      on attachments.parentKey = $self.ID;
    items       : Composition of many Items
                      on items.parentID = $self.ID;
}

entity Items : cuid {
    parentID           : UUID;
    title              : String;
    events             : Association to many Events
                             on events.itemId = $self.ID;
    attachments        : Composition of many Attachments;
    attachmentEntities : Composition of many AttachmentEntity
                             on attachmentEntities.parentKey = $self.ID;
}

entity Events : cuid {
    itemId : UUID;
    items  : Composition of many Items
                 on items.parentID = $self.ID;
}
