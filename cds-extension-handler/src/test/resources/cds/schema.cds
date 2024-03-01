namespace unit.test;

using {cuid} from '@sap/cds/common';
using {com.sap.attachments.Attachments} from '../../../main/resources/cds/com.sap.cds/cds-feature-attachments-handler';

entity Attachment : Attachments {
    parentKey : UUID;
}

entity Roots : cuid {
    title       : String;
    itemTable   : Composition of many Items
                      on itemTable.rootId = $self.ID;
    attachments : Composition of many Attachment
                      on attachments.parentKey = $self.ID;
}

entity Items : cuid {
    rootId      : UUID;
    note        : String;
    events      : Association to many Events;
    attachments : Composition of many Attachment
                      on attachments.parentKey = $self.ID;
}

entity Events {
    key id1        : UUID;
    key id2        : Integer;
        content    : String(100);
        items      : Association to many Items;
        eventItems : Composition of many EventItems;
}

entity EventItems {
    key id1  : UUID;
        note : String;
}

entity wrongAttachment         @(_is_media_data) {
    key ID       : Integer;
        url      : String;
        content  : LargeBinary @Core.MediaType; // only for db-based services
        mimeType : String;
        filename : String;
}

service TestService {
    entity RootTable as
        projection on Roots {
            ID,
            title,
            itemTable   as items,
            attachments as attachmentTable
        };
}
