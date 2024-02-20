namespace com.sap.attachments;

using { cuid, managed } from '@sap/cds/common';

aspect MediaData @(_is_media_data) {
  url      : String;
  content  : LargeBinary; // only for db-based services
  mimeType : String;
}

aspect Attachments : cuid, managed, MediaData {
  filename         : String;
  note             : String;
}
