namespace com.sap.attachments;

using {
    cuid,
    managed
} from '@sap/cds/common';

aspect MediaData @(_is_media_data) {
    content    : LargeBinary; // only for db-based services
    mimeType   : String;
    filename   : String;
    documentId : String;
}

aspect Attachments : cuid, managed, MediaData {
    note : String;
    url  : String;
}
