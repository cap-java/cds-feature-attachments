[![Java Build with Maven](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml/badge.svg)](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build.yml) [![Deploy new Version with Maven](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build-and-deploy.yml/badge.svg?branch=main)](https://github.com/cap-java/cds-feature-attachments/actions/workflows/main-build-and-deploy.yml) [![REUSE status](https://api.reuse.software/badge/github.com/cap-java/cds-feature-attachments)](https://api.reuse.software/info/github.com/cap-java/cds-feature-attachments)

# Attachments Plugin for SAP Cloud Application Programming Model (CAP)

The `com.sap.cds:cds-feature-attachments` dependency is
a [CAP Java plugin](https://cap.cloud.sap/docs/java/building-plugins) that provides out-of-the box attachments storage
and handling by using an aspect Attachments.

## Table of Contents

<!-- TOC -->
* [Additional Information](#additional-information)
* [Support, Feedback, Contributing](#support-feedback-contributing)
* [Minimum Version](#minimum-version)
* [Artifactory](#artifactory)
* [Usage](#usage)
    * [CDS Models](#cds-models)
        * [Model Texts](#model-texts)
        * [Status Texts](#status-texts)
    * [UI](#ui)
    * [Outbox](#outbox)
    * [Malware Scanner](#malware-scanner)
    * [Error Messages](#error-messages)
    * [Restore Endpoint](#restore-endpoint)
        * [Motivation](#motivation)
        * [HTTP Endpoint](#http-endpoint)
        * [Security](#security)
<!-- TOC -->

## Additional Information

- [Process Description](./doc/Processes.md)

- [Changelog](./doc/CHANGELOG.md)

- [Contributing](./doc/CONTRIBUTING.md)

- [License](./LICENSE)

- [Implementation Details](./doc/Design.md)

## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc.
via [GitHub issues](https://github.com/cap-java/cds-feature-attachments/issues).
Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project
structure, as well as additional contribution information,
see our [Contribution Guidelines](./doc/CONTRIBUTING.md).

## Minimum Version

The following version are the minimum versions for the usage of the plugin:

| Component | Minimum Version |
|-----------|-----------------|
| CAP Java  | 3.4.1           |
| UI5       | 1.131.0         |

## Maven Central

The feature is released to Maven Central at:
https://central.sonatype.com/artifact/com.sap.cds/cds-feature-attachments

## Artifactory

Snapshots are deplyoed to SAP's Artifactory in DMZ:
https://common.repositories.cloud.sap/artifactory/cap-java/com/sap/cds/cds-feature-attachments/

The snapshots are also replicated to SAP's internal Artifactory:
https://int.repositories.cloud.sap/artifactory/proxy-cap-java/com/sap/cds/cds-feature-attachments/

If you want to test snapshot versions of this plugin, you need to configure one of these Artifactories in your `${HOME}/.m2/settings.xml`.
See [here](https://maven.apache.org/settings.html#Repositories) for further details.

## Usage

The usage of CAP Java plugins is described in
the [CAP Java Documentation](https://cap.cloud.sap/docs/java/building-plugins#reference-the-new-cds-model-in-an-existing-cap-java-project).
Following this documentation this plugin needs to be referenced in the `srv/pom.xml` of a CAP Java project:

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
using {sap.attachments.Attachments} from `com.sap.cds/cds-feature-attachments`;

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

In addition to the field names also header information (`@UI.HeaderInfo`) are annotated:

| Header Info      | i18n Code     |  
|------------------|---------------|
| `TypeName`       | `attachment`  |
| `TypeNamePlural` | `attachments` |

#### Status Texts

For the status of the attachment only the code value is stored at the moment.
The [status codes](./cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/attachments.cds)
are:

- `Unscanned`
- `Scanning`
- `Clean`
- `Infected`
- `Failed`

If a text for the status needs to be displayed on the UI the model needs to be enhanced with the texts.
For this a new Statuses entity needs to be created like the following example:

```cds
entity Statuses @cds.autoexpose @readonly {
    key code : StatusCode;
        text : localized String(255);
}
```

For this entity csv files can be included in the project structure with texts and translations, to show the translated
texts on the UI.

With this a text can be added in example above like:

```cds
extend Attachments with {
    statusText : Association to Statuses on statusText.code = $self.status;
}
```

With this an annotation can be added to the attachments entity to have the status text displayed in the UI:

```cds
status @(
    Common.Text: {
        $value: ![statusText.text],
        ![@UI.TextArrangement]: #TextOnly
    },
    ValueList: {entity:'Statuses'},
    sap.value.list: 'fixed-values'
);
```

### UI

To enhance the UI with the attachments the following annotations are used for the `UI.Facets` annotations
in your app:

```cds
    {
        $Type  : 'UI.ReferenceFacet',
        ID     : 'AttachmentsFacet',
        Label  : '{i18n>attachmentsAndLinks}',
        Target : 'attachments/@UI.LineItem'
    }
```

A complete `UI.Facets` annotation could look like:

```cds
annotate service.Incidents with @(
    UI.Facets : [
        {
            $Type : 'UI.CollectionFacet',
            Label : '{i18n>Overview}',
            ID : 'Overview',
            Facets : [
                {
                    $Type : 'UI.ReferenceFacet',
                    Label : '{i18n>GeneralInformation}',
                    ID : 'i18nGeneralInformation',
                    Target : '@UI.FieldGroup#i18nGeneralInformation',
                },
                {
                    $Type : 'UI.ReferenceFacet',
                    Label : '{i18n>Details}',
                    ID : 'i18nDetails',
                    Target : '@UI.FieldGroup#i18nDetails',
                }
            ]
        },
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Conversations',
            ID : 'Conversations',
            Target : 'conversations/@UI.LineItem#Conversations',
        },
        {
            $Type  : 'UI.ReferenceFacet',
            ID     : 'AttachmentsFacet',
            Label  : '{i18n>attachmentsAndLinks}',
            Target : 'attachments/@UI.LineItem'
        }
    ]
);
``` 

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

If the default implementation is used and the malware scanner is not available the attachments are marked as clean
by setting the status of the attachment to:

- `Clean`

Only attachments with the status `Clean` are accessible.
Attachments with all other status codes are not accessible.

If the malware scanner is available but during the request to the scanner an error occurs the status of the attachment
is set to:

- `Failed`

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

### Restore Endpoint

The attachment service has an event `RESTORE_ATTACHMENTS`.
This event can be called with a timestamp to restore external stored attachments.

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

There is no predefined endpoint for the restore action.
To call the action of the service from outside the application a service could be defined like the following example:

```cds
service RestoreAttachments {
  action restoreAttachments (restoreTimestamp: Timestamp);
}
```

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

To secure the endpoint security annotations can be used e.g. like the following example:

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
Also, other annotations can be used to secure the service.

More information about the CAP Java security concept can be found in
the [CAP Java Documentation](https://cap.cloud.sap/docs/java/security).
