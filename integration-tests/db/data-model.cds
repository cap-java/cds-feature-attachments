namespace test.data.model;

using {cuid} from '@sap/cds/common';
using {Attachments} from 'com.sap.cds/cds-feature-attachments';
using {Attachment} from 'com.sap.cds/cds-feature-attachments';

entity AttachmentEntity : Attachments {
    parentKey : UUID;
}

entity Roots : cuid {
    title                     : String;
    avatar                    : Attachment;
    coverImage                : Attachment;
    attachments               : Composition of many AttachmentEntity
                                    on attachments.parentKey = $self.ID;
    items                     : Composition of many Items
                                    on items.parentID = $self.ID;
    sizeLimitedAttachments    : Composition of many Attachments;
    mediaValidatedAttachments : Composition of many Attachments;
    mimeValidatedAttachments  : Composition of many Attachments;
    contributors              : Composition of many Contributors
                                    on contributors.rootId = $self.ID;
}

entity Contributors : cuid {
    rootId : UUID;
    name   : String;
}

entity Items : cuid {
    parentID           : UUID;
    title              : String;
    icon               : Attachment;
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
