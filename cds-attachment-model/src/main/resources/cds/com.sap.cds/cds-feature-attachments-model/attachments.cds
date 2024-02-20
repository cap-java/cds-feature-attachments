namespace com.sap.attachments;

using {
    cuid,
    managed
} from '@sap/cds/common';

aspect mediaData                      @(IsMediaData) {
    name               : String;
    content            : LargeBinary  @Core.MediaType: contentType  @Core.ContentDisposition.Filename: name;
    contentType        : String       @Core.IsMediaType;
    externalDocumentId : String       @readonly                     @IsExternalDocumentId; //used for storage of the document id from external providers
}

aspect attachment : cuid, managed, mediaData {
    url  : String @Core.IsURL;
    note : String;
}
