using {com.sap.attachments.MediaData} from './src/main/resources/cds/com.sap.cds/cds-feature-attachments';

@cds.persistence.skip
entity AttachmentServiceInput : MediaData {
    attachmentId: UUID;
    databaseEntityFullName : String;
}
