# Attachments Handler Extension

To handle attachments in this extension a handler was created which shall work for
entities annotated with the annotation `IsMediaData`.

## Example

This example is used in this document:

```cds
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

entity Attachments : attachment {
    parentKey : Integer;
}

entity Roots {
    key ID          : Integer;
        title       : String;
        attachments : Composition of many Attachments
                          on attachments.parentKey = $self.ID;
}

service CatalogService {
    entity Roots       as projection on my.Roots;
}
```

## Handler Interaction with OData

The following table shows the requests from the UI and for which cases the handler shall do something:

| UI Request                                                                        | OData Version | Handler intervention | Event | What needs to be done                                                                                                                             |
|-----------------------------------------------------------------------------------|---------------|----------------------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| POST /odata/v2/CatalogService/Roots(1)/attachments <br /> - JSON Body with data   | V2            | No                   |       |                                                                                                                                                   |
| PUT /odata/v2/CatalogService/Roots(1)/attachments <br /> - Body with file content | V2            | Yes                  | ON    | - Attachment service needs to be called for file creation <br /> - external document id needs to be stored <br /> - file type needs to be updated |
| GET /odata/v2/CatalogService/Roots(1)/attachments                                 | V2            | No                   |       |                                                                                                                                                   |
| GET /odata/v2/CatalogService/Attachments(guid'...')                               | V2            | No                   |       |                                                                                                                                                   |
| GET /odata/v2/CatalogService/Attachments(guid'...')/$value                        | V2            | Yes                  | AFTER | - File content needs to be read and returned                                                                                                      |
| POST /odata/v4/CatalogService/Roots(1)/attachments <br /> - JSON Body with data   | V4            | No                   |       |                                                                                                                                                   |
| PUT /odata/v4/CatalogService/Roots(1)/attachments <br /> - Body with file content | V4            | Yes                  | ON    | - Attachment service needs to be called for file creation <br /> - external document id needs to be stored <br /> - file type needs to be updated |
| GET /odata/v4/CatalogService/Roots(1)/attachments                                 | V4            | No                   |       |                                                                                                                                                   |
| GET /odata/v4/CatalogService/Attachments(...)                                     | V4            | No                   |       |                                                                                                                                                   |
| GET /odata/v4/CatalogService/Attachments(...)/content                             | V4            | Yes                  | AFTER | - File content needs to be read and returned                                                                                                      |



