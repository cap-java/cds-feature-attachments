namespace com.sap.attachments;

using {
    cuid,
    managed
} from '@sap/cds/common';

aspect mediaData                      @(IsMediaData) {
    fileName           : String;
    mimeType           : String       @Core.IsMediaType;
    content            : LargeBinary  @Core.MediaType: mimeType  @Core.ContentDisposition.Filename: fileName;
    externalDocumentId : String       @readonly                  @IsExternalDocumentId; //used for storage of the document id from external providers
}

aspect attachment : cuid, managed, mediaData {
    url  : String @Core.IsURL;
    note : String;
}
