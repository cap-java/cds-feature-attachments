namespace sap.attachments;

using {
    cuid,
    managed
} from '@sap/cds/common';

type StatusCode : String enum {
    Unscanned;
    Scanning;
    Clean;
    Infected;
    Failed;
}

aspect MediaData           @(_is_media_data) {
    content   : LargeBinary; // stored only for db-based services
    mimeType  : String;
    fileName  : String;
    contentId : String     @readonly; // id of attachment in external storage, if database storage is used, same as id
    status    : StatusCode @readonly;
    scannedAt : Timestamp  @readonly;
}

aspect Attachments : cuid, managed, MediaData {
    note : String;
}
