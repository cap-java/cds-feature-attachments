### General Info

This artifact uses the an Object Store as the storage target instead of the underlying database.

Then, the attachment is not stored in the underlying database; instead, it is saved in the respective Object Store, and only a reference to the file is kept in the database, as defined in the [CDS model](../../cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/attachments.cds#L20).

To do this, replace the `cds-feature-attachments` dependency in your `pom.xml` with:

```xml
<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-attachments-oss</artifactId>
    <version>${latest-version}</version>
</dependency>
```

A valid Object Store service binding is also required — for example, one provisioned through SAP BTP.

### Implementation details

This artifact provides custom handlers for events from the [AttachmentService](../../cds-feature-attachments/src/main/java/com/sap/cds/feature/attachments/service/AttachmentService.java).

### Supported Storage Backends

- **AWS S3**
- **Azure Blob Storage**
- **Google Cloud Storage** 

### Multitenancy

Multitenancy is not directly supported. All attachments are stored in a flat structure within the provided bucket, which might be shared across tenants.
