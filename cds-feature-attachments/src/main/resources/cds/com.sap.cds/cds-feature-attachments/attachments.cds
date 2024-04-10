namespace com.sap.attachments;

using {
    cuid,
    managed
} from '@sap/cds/common';

type StatusCode: String enum {
    Unscanned;
    Infected;
    NoScanner;
    Clean;
}

entity Statuses @cds.autoexpose @cds.readonly {
    key code : StatusCode;
    text     : localized String(255);
}
type Status : Association to Statuses;

aspect MediaData @(_is_media_data) {
    content    : LargeBinary; // stored only for db-based services
    mimeType   : String;
    fileName   : String;
    documentId : String;
    status     : Status;
}

aspect Attachments : cuid, managed, MediaData {
    note : String;
    url  : String;
}
