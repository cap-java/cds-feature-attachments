namespace unit.test;

using {cuid} from '@sap/cds/common';
using {sap.attachments.Attachments} from '../../../main/resources/cds/com.sap.cds/cds-feature-attachments';
using from '@sap/cds/srv/outbox';

entity Attachment : Attachments {
}

entity Roots : cuid {
    title              : String;
    itemTable          : Composition of many Items
                             on itemTable.rootId = $self.ID;
    attachments        : Composition of many Attachments;
}

entity Items : cuid {
    rootId          : UUID;
    note            : String;
    events          : Composition of many Events
                          on events.id1 = $self.ID;
    attachments     : Composition of many Attachment on attachments.ID = $self.ID;
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
        stock : Integer;
        sizeLimitedAttachments : Composition of many Attachments;
        defaultSizeLimitedAttachments : Composition of many Attachments;
        minMaxAttachments : Composition of many Attachments;
        minAttachments : Composition of many Attachments;
        maxAttachments : Composition of many Attachments;
        propertyRefAttachments : Composition of many Attachments;
}

annotate EventItems.sizeLimitedAttachments with {
    content @Validation.Maximum: '10KB';
};

annotate EventItems.defaultSizeLimitedAttachments with {
    content @Validation.Maximum;
};

annotate EventItems with {
    minMaxAttachments @Validation.MinItems: 2 @Validation.MaxItems: 5;
    minAttachments @Validation.MinItems: 1;
    maxAttachments @Validation.MaxItems: 3;
    propertyRefAttachments @Validation.MinItems: 'stock';
};

