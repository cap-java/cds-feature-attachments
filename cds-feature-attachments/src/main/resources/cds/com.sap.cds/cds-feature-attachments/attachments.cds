namespace sap.attachments;

using {
    cuid,
    managed,
    sap.common.CodeList
} from '@sap/cds/common';

entity ScanStates : CodeList {
    key code        : String(32)           @Common.Text: name  @Common.TextArrangement: #TextOnly  enum {
            Unscanned;
            Scanning;
            Infected;
            Clean;
            Failed;
        };
        name        : localized String(64) @title: '{i18n>ScanStatus}';
        criticality : Integer              @UI.Hidden;
}

aspect MediaData @(_is_media_data) {
    url       : String;
    content   : LargeBinary; // stored only for db-based services
    mimeType  : String default 'application/octet-stream';
    filename  : String;
    hash      : String;
    contentId : String; // id of attachment in external storage, if database storage is used, same as id
    status    : String default 'Unscanned';
    statusNav : Association to one ScanStates
                    on statusNav.code = status;
    lastScan  : Timestamp;
}

aspect Attachments : cuid, managed, MediaData {
    note : String;
}
