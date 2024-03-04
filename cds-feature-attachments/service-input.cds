using {com.sap.attachments.MediaData} from './src/main/resources/cds/com.sap.cds/cds-feature-attachments';

@cds.persistence.skip
entity AttachmentServiceContext : MediaData {
    attachmentId           : UUID;
    databaseEntityFullName : String;
    isExternalStored       : Boolean;
}
