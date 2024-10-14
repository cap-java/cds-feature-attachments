using {sap.attachments.Attachments} from `com.sap.cds/cds-feature-attachments`;

extend Attachments with {
    // The size of the attachment in bytes
    size : Int32;
}

annotate Attachments with @UI: {
    LineItem  : [
        {Value: content},
        {Value: fileName},
        {Value: size},
        {Value: status},
        {Value: createdAt},
        {Value: createdBy},
        {Value: note},
    ]
} {
    // temporary workaround for issue with etag and Fiori Elements
    modifiedAt @(odata.etag: null);
}
