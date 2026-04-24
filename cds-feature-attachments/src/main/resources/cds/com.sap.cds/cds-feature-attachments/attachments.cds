using {
    cuid,
    managed,
    sap.common.CodeList
} from '@sap/cds/common';

// The common root-level aspect used in applications like that:
// using { Attachments } from 'com.sap.cds/cds-feature-attachments'
aspect Attachments : sap.attachments.Attachments {}

type Attachment : sap.attachments.Attachment;

context sap.attachments {

    type StatusCode : String(32) enum {
        Unscanned;
        Scanning;
        Clean;
        Infected;
        Failed;
    }

    entity ScanStates : CodeList {
        key code        : StatusCode  @Common.Text: name  @Common.TextArrangement: #TextOnly;
            name        : localized String(64);
            criticality : Integer     @UI.Hidden;
    }

    aspect MediaData @(_is_media_data) : managed {
        content   : LargeBinary; // stored only for db-based services
        mimeType  : String default 'application/octet-stream';
        fileName  : String(5000);
        contentId : String                         @readonly; // id of attachment in external storage, if database storage is used, same as id
        status    : StatusCode default 'Unscanned' @readonly;
        scannedAt : Timestamp                      @readonly;
        note      : String(5000);
    }

    type Attachment : MediaData {}

    aspect Attachments : cuid, MediaData {
        statusNav : Association to one ScanStates
                        on statusNav.code = status;
    }
}
