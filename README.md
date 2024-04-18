# Attachments Plugin for SAP Cloud Application Programming Model (CAP)

The `com.sap.cds:cds-feature-attachments` dependency is
a [CAP Java plugin](https://cap.cloud.sap/docs/java/building-plugins) that provides out-of-the box attachments storage
and handling by using an aspect Attachments.

## Table of Contents

<!-- TOC -->

* [Main Build](#main-build)
* [Additional Information](#additional-information)
* [Support, Feedback, Contributing](#support-feedback-contributing)
* [Usage](#usage)
    * [CDS Models](#cds-models)
        * [Model Texts](#model-texts)
    * [Outbox](#outbox)
    * [Malware Scanner](#malware-scanner)
    * [Error Messages](#error-messages)
    * [Status Texts](#status-texts)
    * [Restore Endpoint](#restore-endpoint)
        * [Motivation](#motivation)
        * [HTTP Endpoint](#http-endpoint)
        * [Security](#security)

<!-- TOC -->

## Badge

[![Java Build with Maven](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml/badge.svg)](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml)
[![REUSE status](https://api.reuse.software/badge/github.com/cap-java/cds-feature-attachments)](https://api.reuse.software/info/github.com/cap-java/cds-feature-attachments)

## Additional Information

- [Process Description](./doc/Processes.md)

- [Changelog](./doc/CHANGELOG.md)

- [Contributing](./doc/CONTRIBUTING.md)

- [License](./LICENSE)

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc.
via [GitHub issues](https://github.com/cap-java/cds-feature-attachments/issues).
Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project
structure, as well as additional contribution information,
see our [Contribution Guidelines](./doc/CONTRIBUTING.md).

## Usage

The usage of CAP Java plugins is described in
the [CAP Java Documentation](https://cap.cloud.sap/docs/java/building-plugins#reference-the-new-cds-model-in-an-existing-cap-java-project).
Following this documentation this plugin needs to be referenced in the `pom.xml` of a CAP Java project:

```xml

<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-attachments</artifactId>
    <version>${latest-version}</version>
</dependency>
```

The latest version can be found in the [changelog](./doc/CHANGELOG.md).

To be able to also use the cds models defined in this plugin the `cds-maven-plugin` needs to be used with the
`resolve` goal to make the cds models available in the project:

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

If the cds models needs to be used in the `db` folder the `cds-maven-plugin` needs to be included also in the
`db` folder of the project.
This means the `db` folder needs to have a `pom.xml` with the `cds-maven-plugin` included and the `cds-maven-plugin`
needs to be run.

If the `cds-maven-plugin` is used correctly and executed the following lines should be visible in the build log:

````log
[INFO] --- cds:2.8.1:resolve (cds.resolve) @ your-project-srv ---
[INFO] CdsResolveMojo: Copying models from com.sap.cds:cds-feature-attachments:<latest-version> (<project-folder>\target\classes)
````

After that the models can be used.

### CDS Models

The cds models in include an aspect to add attachments to entities.
It also includes annotations used by the FIORI UI to handle attachments.

To use the aspect the following code needs to be added to the entity definition:

```cds
using {com.sap.attachments.Attachments} from `com.sap.cds/cds-feature-attachments`;

entity Items : cuid {
    ...
    attachments : Composition of many Attachments;
    ...
}
```

The aspect `Attachments` shall be used directly for the composition.
It is important to use the correct from clause for the `using` statement.

Only if `com.sap.cds/cds-feature-attachments` is used and not concrete files of the feature are specified in the
from-statement also the annotations and other definitions are found and used.

#### Model Texts

In the model several fields are annotated with the `@title` annotation.
The texts of these fields needs to be included in the consumings project UI.

The following table gives an overview of the fields and the i18n codes:

| Field Name | i18n Code             |
|------------|-----------------------|
| `content`  | `attachment_content`  |
| `mimeType` | `attachment_mimeType` |
| `fileName` | `attachment_fileName` |
| `status`   | `attachment_status`   |
| `note`     | `attachment_note`     |
| `url`      | `attachment_url`      |

In addition to the field names also header information (`@UI.HeaderInfo`) are annotated:

| Header Info      | i18n Code     |  
|------------------|---------------|
| `TypeName`       | `attachment`  |
| `TypeNamePlural` | `attachments` |

### Outbox

In this plugin the [persistent outbox](https://cap.cloud.sap/docs/java/outbox#persistent) is used to mark attachments as
deleted.
The enablement of the outbox is also included in the cds models of this plugin.
In the capire documentation of the [persistent outbox](https://cap.cloud.sap/docs/java/outbox#persistent) is it
described how to overwrite
the default outbox configuration.

If the default shall be used, nothing needs to be done.

### Malware Scanner

This plugin checks for a binding to
the [SAP Malware Scanning Service](https://help.sap.com/docs/malware-scanning-servce).
The concrete check if for a binding to a service with label `malware-scanner`.

The malware scanner is used in the default implementation of the technical service `AttachmentService` to scan
attachments.
If the default implementation of this service is overwritten, e.g. by using the plugin enhancement of the
[SAP Document Management Service](https://help.sap.com/docs/document-management-service), then this overwriting plugin
is responsible for the malware scan and the plugin documentation needs to be checked for how the malware scan is done.

If the default implementation is used and the malware scanner is not available the attachments are marked as not scanned
by setting the status of the attachment to:

- `NO_SCANNER`

Attachments with this status are accessible like attachments with the status `CLEAN`.
Only attachments with the following status are not accessible:

- `INFECTED`
- `UNSCANNED`

### Error Messages

If attachments are uploaded but not scanned by a malware scanner (if a scanner is available) or are marked as infected
the direct access of the
attachment is not possible.
In case users try to access the content of the attachment the following errors messages are displayed:

| Error Message                                         | Error Message i18n Code |
|-------------------------------------------------------|-------------------------|
| Attachment is not clean                               | `not_clean`             |
| Attachment is not scanned, try again in a few minutes | `not_scanned`           |

By adding the error message i18n code to the `i18n.properties` file the error message can be overwritten translated.
More information can be found in the capire documentation
for [i18n](https://cap.cloud.sap/docs/guides/i18n#where-to-place-text-bundles).

### Status Texts

The status of an attachments, which is the malware scan status is also included in the cds models.
It is annotated in a way, that it is included in the UI table to show the attachments.
The default text of the status is also included in the cds models.

If the resolve goal of the `cds-maven-plugin` is executed the following files should be available in the csv file folder
of the `db` module:

- `com.sap.attachments-Statuses.csv`
- `com.sap.attachments-Statuses.hdbtabledata`
- `com.sap.attachments-Statuses-texts.csv`
- `com.sap.attachments-Statuses_texts.hdbtabledata`

In addition, the following files are included in the table definitions folder:

- `com.sap.attachments.Statuses.hdbtable`
- `com.sap.attachments.Statuses_texts.hdbtable`

The default texts of the status are:

```csv
CODE,TEXT
UNSCANNED,Not Scanned
INFECTED,Infected
NO_SCANNER,No Scanner Available
CLEAN,Clean
```

If the texts need to be overwritten or translated files for the translated texts can be added like:

- `com.sap.attachments-Statuses-texts_en.csv`

### Restore Endpoint

#### Motivation

Documents which are marked as deleted can be restored.

The use case behind this feature is:

If backups of databases are restored the attachments stored in external storages also needs to be restored.
To have a possibility to restore attachments which are marked as deleted a restore endpoint is available.

In the default implementation of the technical service `AttachmentService` this is not needed as the attachments are
stored directly in the database and are restored with the database.

If the default implementation is overwritten, e.g. by using the plugin enhancement of the
[SAP Document Management Service](https://help.sap.com/docs/document-management-service), then this overwriting plugin
needs to handle the restore of attachments.

In such cases the restore endpoint can be used to restore attachments.

How long attachments are marked as deleted before they get deleted dependents on the configuration
of the used storage.

#### HTTP Endpoint

Within the cds model
a [service](./cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/restore-service.cds) is
defined with a single action:

```cds
namespace com.sap.attachments;

service RestoreAttachments {
  action restoreAttachments (restoreTimestamp: Timestamp);
}
```

The action `restoreAttachments` gets in a timestamp from which the documents need to be restored.
The action is called with a POST request to the endpoint:

- OData v4: `/odata/v4/com.sap.attachments.RestoreAttachments/restoreAttachments`
- OData v2: `/odata/v2/com.sap.attachments.RestoreAttachments/restoreAttachments`

With the body:

```json
{
  "restoreTimestamp": "2024-04-17T10:36:38.813491100Z"
}
```

#### Security

For the restore endpoint no security configuration is delivered as the depends on the using services.
To secure the endpoint security annotations can be used e.g. like the following example:

```cds
using {com.sap.attachments.Attachments,
       com.sap.attachments.RestoreAttachments} from `com.sap.cds/cds-feature-attachments`;

entity Items : cuid {
    ...
    attachments : Composition of many Attachments;
    ...
}

annotate RestoreAttachments with @(requires: 'authenticated-user');
```

Here the `RestoreAttachments` service is annotated with the `requires` annotation to secure the service.
Also, other annotations can be used to secure the service.

More information about the CAP Java security concept can be found in
the [CAP Java Documentation](https://cap.cloud.sap/docs/java/security).
