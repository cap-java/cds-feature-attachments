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

A valid Object Store service binding is required for this — for example, one provisioned through SAP BTP.

#### Tests

The unit tests in this module do not need a binding to the respective object stores, run them with `mvn clean install`.

The integration tests need a binding to a real object store, run them with `mvn clean install -Pintegration-tests-oss`.
To set the binding, provide the following environment variables:
- AWS_S3_BUCKET
- AWS_S3_REGION
- AWS_S3_ACCESS_KEY_ID
- AWS_S3_SECRET_ACCESS_KEY
- AZURE_CONTAINER_URI
- AZURE_SAS_TOKEN
- GS_BUCKET
- GS_PROJECT_ID
- GS_BASE_64_ENCODED_PRIVATE_KEY_DATA

### Implementation details

This artifact provides custom handlers for events from the [AttachmentService](../../cds-feature-attachments/src/main/java/com/sap/cds/feature/attachments/service/AttachmentService.java).

### Supported Storage Backends

- **AWS S3**
- **Azure Blob Storage**
- **Google Cloud Storage** 

### Multitenancy

Multitenancy is not directly supported. All attachments are stored in a flat structure within the provided bucket, which might be shared across tenants.
