namespace unit.test;

using { com.sap.attachments.Attachments } from '../../../main/resources/cds/com.sap.cds/cds-feature-attachments-handler';

entity Attachment : Attachments {
    parentKey : Integer;
}

entity Roots {
    key ID          : Integer;
        title       : String;
        items       : Composition of many Items
                          on items.rootID = $self.ID;
        attachments : Composition of many Attachment
                          on attachments.parentKey = $self.ID;
}

entity Items {
    key ID          : Integer;
        rootID      : Integer;
        title       : String;
        attachments : Composition of many Attachment
                          on attachments.parentKey = $self.ID;
}
