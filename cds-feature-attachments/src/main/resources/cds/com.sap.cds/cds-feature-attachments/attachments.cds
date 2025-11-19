namespace sap.attachments;

using {
    cuid,
    managed,
    sap.common.CodeList
} from '@sap/cds/common';

type StatusCode : String(32) enum {
    Unscanned;
    Scanning;
    Clean;
    Infected;
    Failed;
}

aspect MediaData @(_is_media_data) {
    content   : LargeBinary; // stored only for db-based services
    mimeType  : String;
    fileName  : String;
    contentId : String     @readonly; // id of attachment in external storage, if database storage is used, same as id
    status    : StatusCode default 'Unscanned' @readonly;
    statusNav : Association to one ScanStates on statusNav.code = status;
    scannedAt : Timestamp  @readonly;
}

entity ScanStates : CodeList {
    key code        : StatusCode  @Common.Text: name  @Common.TextArrangement: #TextOnly;
        name        : localized String(64);
        criticality : Integer     @UI.Hidden;
}

aspect Attachments : cuid, managed, MediaData {
    note : String;
}
