[![Java Build with Maven](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml/badge.svg)](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml) [![Deploy new Version with Maven](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml/badge.svg?branch=main)](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml) [![REUSE status](https://api.reuse.software/badge/github.com/cap-java/cds-feature-attachments)](https://api.reuse.software/info/github.com/cap-java/cds-feature-attachments)

# Attachments Plugin for SAP Cloud Application Programming Model (CAP)

The `com.sap.cds:cds-feature-attachments` dependency is
a [CAP Java plugin](https://cap.cloud.sap/docs/java/building-plugins) that provides out-of-the box attachments storage
and handling by using an aspect Attachments.

It supports the [AWS, Azure and Google object stores](storage-targets/cds-feature-attachments-oss) and can connect to a [malware scanner](#malware-scanner).

## Table of Contents

<!-- TOC -->

* [Quick Start](#quick-start)
* [Usage](#usage)
  * [MVN Setup](#mvn-setup)
  * [Changes in the CDS Models and for the UI](#changes-in-the-cds-models-and-for-the-UI)
  * [Storage Targets](#storage-targets)
  * [Malware Scanner](#malware-scanner)
  * [Outbox](#outbox)
  * [Restore Endpoint](#restore-endpoint)
    * [Motivation](#motivation)
    * [HTTP Endpoint](#http-endpoint)
    * [Security](#security)
* [Releases: Maven Central and Artifactory](#releases-maven-central-and-artifactory)
* [Minimum UI and CAP Java Version](#minimum-ui5-and-cap-java-version)
* [Architecture Overview](#architecture-overview)
  * [Design](#design)
  * [Multitenancy](#multitenancy)
  * [Object Stores](#object-stores)
  * [Model Texts](#model-texts)
* [Monitoring & Logging](#monitoring--logging)
* [Support, Feedback, Contributing](#support-feedback-contributing)
* [References & Links](#references--links)

## Quick Start

For a quick setup with in-memory storage:
- Add the `cds-feature-attachments` Maven dependency to the `srv/pom.xml` and configure the `cds-maven-plugin` with the `resolve` goal as described in [MVN Setup](#mvn-setup).
- Extend the CDS model with the `Attachments` aspect and annotate the service for UI integration as explained in [Changes in the CDS Models and for the UI](#changes-in-the-cds-models-and-for-the-UI).

The [incidents app](https://github.com/cap-java/incidents-app/) provides a demonstration of how to use this plugin.

For object store integration, see [Amazon, Azure, and Google Object Stores](storage-targets/cds-feature-attachments-oss).

## Usage

### MVN Setup

As described in the [CAP Java Documentation](https://cap.cloud.sap/docs/java/building-plugins#reference-the-new-cds-model-in-an-existing-cap-java-project), the attachments plugin needs to be referenced in the `srv/pom.xml` of the consuming CAP Java application:

```xml
<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-attachments</artifactId>
    <version>${latest-version}</version>
</dependency>
```
Additionally, the `cds-maven-plugin` must be configured with the `resolve` goal to ensure CDS models from dependencies are available.
For this, add the following to the `srv/pom.xml` before the entry `build` as well:

```xml
<plugin>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-maven-plugin</artifactId>
    <version>${cds.services.version}</version>
    <executions>
        <execution>
            <id>cds.resolve</id>
            <goals>
                <goal>resolve</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
After that, the aspect `Attachments` can be used in the application's CDS model.

### Changes in the CDS Models and for the UI

To use the aspect `Attachments` on an existing entity, the corresponding entity needs to be extended in a CDS file in the `srv` module.
The following example shows how to extend the entity `Incidents` in the `srv` module with an additional `attachments.cds` file, it also directly adds the respective UI Facet.
To use this file with the [incidents app](https://github.com/cap-java/incidents-app/), check out the source code, copy the [file from the xmpls folder](https://github.com/cap-java/incidents-app/blob/main/xmpls/attachments.cds) to the srv folder and run the app as explained in the [incidents app README](https://github.com/cap-java/incidents-app/blob/main/README.md).

  ```cds
  using { sap.capire.incidents as my } from '../db/schema';
  using { sap.attachments.Attachments } from 'com.sap.cds/cds-feature-attachments';

  extend my.Incidents with {
    attachments: Composition of many Attachments;
  }

  using { ProcessorService as service } from '../app/services';
  annotate service.Incidents with @(
    UI.Facets: [
      ...,
      {
        $Type  : 'UI.ReferenceFacet',
        ID     : 'AttachmentsFacet',
        Label  : '{i18n>attachments}',
        Target : 'attachments/@UI.LineItem'
      }
    ]
  );
  ```

The UI Facet can also be added directly after other UI Facets in a `cds` file in the `app` folder.

### Storage Targets

By default, the plugin operates without a dedicated storage target, storing attachments directly in the [underlying database](cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/attachments.cds#L17).

Other available storage targets:
- [Amazon, Azure and Google Object Stores](storage-targets/cds-feature-attachments-oss)
- [local file system as a storage backend](storage-targets/cds-feature-attachments-fs) (only for testing scenarios)

When using a dedicated storage target, the attachment is not stored in the underlying database; instead, it is saved on the specified storage target and only a reference to the file is kept in the database, as defined in the [CDS model](cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/attachments.cds#L20).

### Malware Scanner

This plugin checks for a binding to
the [SAP Malware Scanning Service](https://help.sap.com/docs/malware-scanning-servce), this needs to have the label `malware-scanner`. The entry in the [mta-file](https://cap.cloud.sap/docs/guides/deployment/to-cf#add-mta-yaml) may look like:

```
_schema-version: '0.1'
ID: consuming-app
version: 1.0.0
description: "App consuming the attachments plugin with a malware scanner"
parameters:
  ...
modules:
  - name: consuming-app-srv
# ------------------------------------------------------------
    type: java
    path: srv
    parameters:
      ...
    properties:
      ...
    build-parameters:
      ...
    requires:
      - name: consuming-app-hdi-container
      - name: consuming-app-uaa
      - name: cf-logging
      - name: malware-scanner
...
resources:
  ...
  - name: malware-scanner
    type: org.cloudfoundry.managed-service
    parameters:
      service: malware-scanner
      service-plan: clamav
```

The malware scanner is used in the `AttachmentService` to scan
attachments.

If there is no malware scanner available, the attachments are automatically marked as `Clean`.

Scan status codes:
- `Clean`: Only attachments with the status `Clean` are accessible.
- `Scanning`: Immediately after upload, the attachment is marked as `Scanning`. Depending on processing speed, it may already appear as `Clean` when the page is reloaded.
- `Unscanned`: Attachment is still unscanned.
- `Failed`: Scanning failed.
- `Infected`: The attachment is infected.


### Outbox

In this plugin the [persistent outbox](https://cap.cloud.sap/docs/java/outbox#persistent) is used to mark attachments as
deleted.
When using this plugin, the persistent outbox is enabled by default.
In the capire documentation of the [persistent outbox](https://cap.cloud.sap/docs/java/outbox#persistent) is it
described how to overwrite
the default outbox configuration.

If the default shall be used, nothing needs to be done.


### Restore Endpoint

The attachment service has an event `RESTORE_ATTACHMENTS`.
This event can be called with a timestamp to restore externally stored attachments.

#### Motivation

Documents which are marked as deleted can be restored.

The use cases behind this feature are:
- Restoring attachments after a database backup is restored:
When restoring a database backup, any attachments stored in external storage (object stores, etc.) also need to be restored to maintain data consistency.
- Restoring attachments that were marked as deleted:
The restore endpoint provides a way to recover attachments that were previously marked as deleted, making it possible to undo deletions if needed.

In the default implementation of the technical service `AttachmentService` this is not needed as the attachments are
stored directly in the database and are restored with the database.

If the default implementation is replaced by overwriting the [respective handler](cds-feature-attachments/src/main/java/com/sap/cds/feature/attachments/service/handler/DefaultAttachmentsServiceHandler.java#L87), for example by the
[SAP Document Management Service](https://help.sap.com/docs/document-management-service), then the overwriting plugin
needs to handle the restore of attachments.

In such cases the restore endpoint can be used to restore attachments.

How long attachments are marked as deleted before they are actually deleted depends on the configuration of the used storage.

#### HTTP Endpoint

There is no predefined endpoint for the restore action.
To call the action of the service from outside the application a service could be defined as in the following example:

```cds
service RestoreAttachments {
  action restoreAttachments (restoreTimestamp: Timestamp);
}
```

See [Security](#security) for how to secure this endpoint.
The action `restoreAttachments` could get in a timestamp from which the attachments need to be restored.
The action could be called with a POST request to the endpoint:

- OData v4: `/odata/v4/RestoreAttachments/restoreAttachments`
- OData v2: `/odata/v2/RestoreAttachments/restoreAttachments`

With the body:

```json
{
  "restoreTimestamp": "2024-04-17T10:36:38.813491100Z"
}
```

The action needs to be implemented and can call the attachment service as in the following example:

```java

@ServiceName(RestoreAttachments_.CDS_NAME)
public class RestoreAttachmentsHandler implements EventHandler {

	private final AttachmentService attachmentService;

	public RestoreAttachmentsHandler(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@On(event = RestoreAttachmentsContext.CDS_NAME)
	public void restoreAttachments(RestoreAttachmentsContext context) {
		attachmentService.restoreAttachment(context.getRestoreTimestamp());
		context.setCompleted();
	}

}
```

In the Spring Boot context the `AttachmentService` can be autowired in the handler.

#### Security

To secure the endpoint, security annotations can be used. For example:

```cds
using {sap.attachments.Attachments} from `com.sap.cds/cds-feature-attachments`;

entity Items : cuid {
    ...
    attachments : Composition of many Attachments;
    ...
}

annotate RestoreAttachments with @(requires: 'internal-user');
```

Here the `RestoreAttachments` service is annotated with the `requires` annotation to secure the service.
Various other annotations can be used to secure the service.

More information about the CAP Java security concept can be found in
the [CAP Java Documentation](https://cap.cloud.sap/docs/java/security).

## Releases: Maven Central and Artifactory

- The plugin is released to Maven Central at: https://central.sonatype.com/artifact/com.sap.cds/cds-feature-attachments (public access).
- See the [changelog](./doc/CHANGELOG.md) for the latest changes.

- To test snapshot versions of this plugin, the artifactory in `${HOME}/.m2/settings.xml` needs to be configured. See [the maven settings](https://maven.apache.org/settings.html#Repositories) for further details.

## Minimum UI5 and CAP Java Version

| Component | Minimum Version |
|-----------|-----------------|
| CAP Java  | 3.10.3          |
| UI5       | 1.136.0         |


## Architecture Overview
### Design
- [Design Details](./doc/Design.md)
- [Process of Creating, Reading and Deleting an Attachment](./doc/Processes.md)

### Multitenancy

- When using SAP HANA as the storage target, multitenancy support depends on the consuming application. In most cases, multitenancy is achieved by using a dedicated schema for each tenant, providing strong data isolation at the database level.
- When using an [object store](storage-targets/cds-feature-attachments-oss) as the storage target, true multitenancy is not yet implemented (as of version 1.2.1). In this case, all blobs are stored in a single bucket, and tenant data is not separated.

### Object Stores

See [Object Stores](storage-targets/cds-feature-attachments-oss).

### Model Texts

In the model, several fields are annotated with the `@title` annotation. Default texts are provided in [35 languages](https://github.com/cap-java/cds-feature-attachments/tree/main/cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/_i18n). If these defaults are not sufficient for an application, they can be overwritten by applications with custom texts or translations.

The following table gives an overview of the fields and the i18n codes:

| Field Name | i18n Code             |
|------------|-----------------------|
| `content`  | `attachment_content`  |
| `mimeType` | `attachment_mimeType` |
| `fileName` | `attachment_fileName` |
| `status`   | `attachment_status`   |
| `note`     | `attachment_note`     |

In addition to the field names, header information (`@UI.HeaderInfo`) are also annotated:

| Header Info      | i18n Code     |  
|------------------|---------------|
| `TypeName`       | `attachment`  |
| `TypeNamePlural` | `attachments` |


## Monitoring & Logging

To configure logging for the attachments plugin, add the following line to the `/srv/src/main/resources/application.yaml` of the consuming application:
```
logging:
  level:
    ...
    '[com.sap.cds.feature.attachments]': DEBUG
...
```

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc.
via [GitHub issues](https://github.com/cap-java/cds-feature-attachments/issues).

Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project
structure, as well as additional contribution information,
see our [Contribution Guidelines](./doc/CONTRIBUTING.md).

## References & Links
- [License](./LICENSE)
