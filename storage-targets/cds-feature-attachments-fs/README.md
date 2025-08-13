### General Info

This artifact uses the local file system as the storage target instead of the underlying database.

Then, the attachment is not stored in the underlying database; instead, it is saved on the local file system, and only a reference to the file is kept in the database, as defined in the [CDS model](../../cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/attachments.cds#L20).

To do this, replace the `cds-feature-attachments` dependency in your `pom.xml` with:

```xml
<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-attachments-fs</artifactId>
    <version>${latest-version}</version>
</dependency>
```

**This storage target is not supposed for productive usage, only for testing purposes!**

### Implementation details

This artifact provides custom handlers for events from the [AttachmentService](../../cds-feature-attachments/src/main/java/com/sap/cds/feature/attachments/service/AttachmentService.java).