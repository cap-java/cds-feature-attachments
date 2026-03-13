namespace test.data.model;

using {cuid} from '@sap/cds/common';
using {sap.attachments.Attachments} from `com.sap.cds/cds-feature-attachments`;

entity AttachmentEntity : Attachments {
    parentKey : UUID;
}

// Simple entity for testing @Validation.MaxItems without inheriting from Attachments.
// This avoids affecting attachment event counts in existing tests.
entity MaxLimitedItem : cuid {
    name      : String;
    parentKey : UUID;
}

entity Roots : cuid {
    title       : String;
    attachments : Composition of many AttachmentEntity
                      on attachments.parentKey = $self.ID;
    items       : Composition of many Items
                      on items.parentID = $self.ID;
    sizeLimitedAttachments : Composition of many Attachments;
    @Validation.MaxItems: 3
    maxLimitedAttachments : Composition of many MaxLimitedItem
                      on maxLimitedAttachments.parentKey = $self.ID;
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
