namespace unit.test;

using {cuid} from '@sap/cds/common';
using {sap.attachments.Attachments} from '../../../main/resources/cds/com.sap.cds/cds-feature-attachments';
using from '@sap/cds/srv/outbox';

entity Attachment : Attachments {
    parentKey : UUID;
}

entity Roots : cuid {
    title       : String;
    itemTable   : Composition of many Items
                      on itemTable.rootId = $self.ID;
    attachments : Composition of many Attachments;
}

entity Items : cuid {
    rootId          : UUID;
    note            : String;
    events          : Composition of many Events
                          on events.id1 = $self.ID;
    attachments     : Composition of many Attachment
                          on attachments.parentKey = $self.ID;
    itemAttachments : Composition of many Attachments;
}

entity Events {
    key id1        : UUID;
    key id2        : Integer;
        content    : String(100);
        items      : Association to many Items
                         on items.ID = $self.id1;
        itemsCompo : Association to many Items
                         on itemsCompo.ID = $self.id1;
        eventItems : Composition of many EventItems
                         on eventItems.id1 = $self.id1;
}

entity EventItems {
    key id1  : UUID;
        note : String;
}
