# Implementation Details

## Table of Contents

<!-- TOC -->

- [Links for Design, Processes and Readme](#links-for-design-processes-and-readme)
- [Folder Structure](#folder-structure)
- [GitHub Actions](#github-actions)
  - [Build Action](#build-action)
  - [Pull Requests Build](#pull-requests-build)
    - [Trigger](#trigger)
  - [Main Build](#main-build)
    - [Trigger](#trigger-1)
  - [Build and Deploy](#build-and-deploy)
    - [Trigger](#trigger-2)
    - [Repository for Deploy](#repository-for-deploy)
    - [Update Version](#update-version)
      - [Token for Version Update](#token-for-version-update)
  - [BlackDuck](#blackduck)
    - [Pull Requests](#pull-requests)
    - [BlackDuck Links](#blackduck-links)
  - [Secrets](#secrets)
- [Feature](#feature)
  - [CDS Model](#cds-model)
    - [ETag](#etag)
    - [Usage of the CDS Model](#usage-of-the-cds-model)
  - [Configuration](#configuration)
  - [Handler](#handler)
    - [Mapping CAP Events to Attachment Operations](#mapping-cap-events-to-attachment-operations)
    - [Draft Activate and Deep Updates](#draft-activate-and-deep-updates)
    - [Content for new Draft](#content-for-new-draft)
    - [Delete](#delete)
    - [Draft Keys](#draft-keys)
    - [Sibling Entity (Draft or Active)](#sibling-entity-draft-or-active)
    - [Readonly Fields](#readonly-fields)
    - [Optimistic Concurrency Control](#optimistic-concurrency-control)
  - [Service](#service)
    - [Service Interface](#service-interface)
    - [Multi-Tenancy](#multi-tenancy)
    - [Default Implementation](#default-implementation)
      - [Content ID](#content-id)
    - [Malware Scan](#malware-scan)
      - [User Experience](#user-experience)
      - [Implementation](#implementation)
        - [Read Data](#read-data)
        - [Scan Content](#scan-content)
        - [Store Scan Result](#store-scan-result)
        - [Read Attachment calls Malware Scan](#read-attachment-calls-malware-scan)
      - [Status](#status)
  - [Texts](#texts)
- [Tests](#tests)
  - [Unit Tests](#unit-tests)
  - [Integration Tests](#integration-tests)
- [Quality Tools](#quality-tools)
<!-- TOC -->

## Links for Design, Processes and Readme

- [Bulk License Documentation](https://reuse.software/faq/#bulk-license)
- [CAP Java Tutorial](https://cap.cloud.sap/docs/java/getting-started)
- [CAP Java plugin concept](https://cap.cloud.sap/docs/java/building-plugins#building-plugins)
- [CAP Java Outbox documentation](https://cap.cloud.sap/docs/java/outbox#outboxing-cap-service-events)
- [CAP Java Security Documentation](https://cap.cloud.sap/docs/java/security)
- [CAP Notebooks](https://cap.cloud.sap/docs/tools/#cap-vscode-notebook)
- [BlackDuck Scan Results](https://sap.blackducksoftware.com/api/projects/480aae67-284b-42cf-840c-ba021be84378)
- [BlackDuck User Groud Self Service](https://svmprod-zdohvhnx0v.dispatcher.int.sap.eu2.hana.ondemand.com/webapp/index.html)
- [Artifactory](https://common.repositories.cloud.sap/ui/repos/tree/General/cap-java)
- [SAP Malware Scanning Service](https://help.sap.com/docs/malware-scanning-servce)
- [SAP Document Management Service](https://help.sap.com/docs/document-management-service)

## Folder Structure

The following tables shows the folder structure of the project.

| Folder                                      | Description                                                                                                                  |
| ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| .github                                     | GitHub Actions definitions and the configuration of the dependabot                                                           |
| .pipeline                                   | Project Piper configuration, which is used in the GitHub Actions. Piper per default searches in this folder for `config.yml` |
| cds-feature-attachments                     | Core implementation of the attachments feature                                                                               |
| doc                                         | Design documents and process descriptions                                                                                    |
| integration-tests                           | Spring Boot integration tests for the feature                                                                                |
| LICENSES                                    | License descriptions                                                                                                         |
| samples/bookshop                            | Sample CAP Java application demonstrating attachments usage                                                                  |
| storage-targets/cds-feature-attachments-fs  | File system storage target (for testing scenarios only)                                                                      |
| storage-targets/cds-feature-attachments-oss | Object Store storage target (AWS S3, Azure Blob, Google Cloud Storage)                                                       |

## GitHub Actions

In folder `.github/workflows` are the GitHub Actions defined. The following table shows the actions and their purpose.

| File Name      | Description                                                                                                                                                           |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `pr.yml`       | Builds and tests pull requests for Java 17 and 21. Requires approval for external forks. Each pull request needs green runs from this workflow to be merged.          |
| `main.yml`     | Builds and tests when commits are merged to main. Runs unit tests and integration tests for Java 17 and 21.                      |
| `release.yml`  | Triggered on GitHub releases. Updates version, runs BlackDuck scan, builds, tests, and deploys to Maven Central. See also [Build and Deploy](#build-and-deploy).      |
| `pipeline.yml` | Reusable workflow containing shared build, test, integration test, SonarQube scan, CodeQL analysis, and snapshot deployment logic. Called by `pr.yml` and `main.yml`. |

### Build Action

The build step is implemented in action `.github/actions/build/action.yml` which is used in the workflows via `pipeline.yml`.

Additional reusable actions are defined in `.github/actions/`:

| Action                | Description                                                 |
| --------------------- | ----------------------------------------------------------- |
| `build`               | Builds the project and runs unit tests                      |
| `integration-tests`   | Runs integration tests (build-version, latest-version, oss) |
| `deploy-release`      | Deploys release artifacts to Maven Central                  |
| `newrelease`          | Updates version in pom.xml for new releases                 |
| `scan-with-blackduck` | Runs BlackDuck vulnerability scans                          |
| `scan-with-sonar`     | Runs SonarQube code analysis                                |

### Pull Requests Build

The `pr.yml` starts a workflow to build the project and run all unit and Spring Boot tests for the
coding in the new branch including the changes in the pull request.

#### Trigger

This workflow is triggered if a new pull request is created or updated in GitHub.
For pull requests from external forks, manual approval is required before the workflow runs.

### Main Build

The `main.yml` starts a workflow to build the project and run all unit and Spring Boot tests for the main branch.
It also deploys snapshot versions to Artifactory.

#### Trigger

This workflow is triggered if a new commit is pushed to the main branch.

### Build and Deploy

The [`release.yml`](../.github/workflows/release.yml) starts a workflow to build the project, run all tests and deploy it to Maven Central.
The workflow is started if a new release is created in GitHub. The tag used in the release is used as the new version for the project.

The following steps are executed in the workflow:

1. Update the version in the `pom.xml` files. The tag used in the release is read and git commands are used to update
   the property `revision` in the parent `pom.xml` file.
2. Build the project and run all unit and integration tests. Here a reuse action is used which is also
   executed in the main and pull request build.
3. Deploy the project to maven or artifactory. The deployment is done with the maven command `mvn deploy`. The
   deployment is done to the repository defined in the `pom.xml` file. So only project parts which have defined the
   repository are deployed.
   With this the deployment is done only for the `cds-feature-attachments` and the root project but not for the
   integration tests.

During deploy the following properties are used:

- `maven.install.skip=true`
- `maven.test.skip=true`

#### Trigger

This workflow is triggered if a new release is created in GitHub.
With this a new version is published to Maven Central once a new release is created in GitHub.

#### Repository for Deploy

In the root `pom.xml` the repository for the deployment is defined. The following code snippet shows the repository definition in the `pom.xml` file.

```xml
<distributionManagement>
    <repository>
        <id>central</id>
        <name>MavenCentral</name>
        <url>https://central.sonatype.com</url>
    </repository>
    <snapshotRepository>
        <id>artifactory</id>
        <name>Artifactory_DMZ-snapshots</name>
        <url>https://common.repositories.cloud.sap/artifactory/cap-java</url>
    </snapshotRepository>
</distributionManagement>
```

Release versions are deployed to Maven Central, while snapshot versions are deployed to SAP's Artifactory.
Only the root `pom.xml` and the `cds-feature-attachments/pom.xml` have defined these repositories as
only these modules shall be deployed. The storage target modules (`cds-feature-attachments-oss`, `cds-feature-attachments-fs`) are also deployed.
If the root `pom.xml` is not deployed the usage of the `cds-feature-attachments/pom.xml` is not possible.

#### Update Version

Inside the `release.yml` an action is called to create a new project version in the parent `pom.xml`
of the project.
The version is taken from the tag of the newly created release in GitHub.
The version is updated in the `revision` property in the parent `pom.xml` file.

##### Token for Version Update

With the current setup of the GitHub repository it is only possible to merge changes directly in the `main`
branch with an administrator account.
As the default GitHub token which is used in GitHub actions this is not possible. Because of this a
token was created and stored in the GitHub secrets under `GH_TOKEN`.
This token is used in the workflow to update the version in the `pom.xml` files.

<!-- ### BlackDuck

The BlackDuck action is called in the `main.yaml`, `main-build-and-deploy.yaml` and `main-build-and-deploy-oss.yaml` to check the project for vulnerabilities.
The action is defined in the `../.github/actions/scan-with-blackduck/action.yaml` file. The action uses the project piper action to call BlackDuck.

The following user group is used for the BackDuck scan:

- `CDSJAVA-OPEN-SOURCE`

The group and other settings are defined in the project piper [config](../.pipeline/config.yml) file. -->

<!-- #### Pull Requests
The scan is not used in pull request builds as for the scan a token is needed and this token is
not available in the pull request builds from fork projects or dependabot pull requests.

#### BlackDuck Links

- [Scan Results](https://sap.blackducksoftware.com/api/projects/480aae67-284b-42cf-840c-ba021be84378)
- [User Groud Self Service](https://svmprod-zdohvhnx0v.dispatcher.int.sap.eu2.hana.ondemand.com/webapp/index.html) -->

### Secrets

The following secrets are stored in GitHub and used in the GitHub-actions:

| Secret Name             | Description                                                                                                                                                         |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BLACK_DUCK_TOKEN        | Token for calling BlackDuck to execute BlackDuck scans.                                                                                                             |
| DEPLOYMENT_USER         | User for snapshot deployment to [Artifactory](https://common.repositories.cloud.sap/ui/repos/tree/General/cap-java).                                                |
| DEPLOYMENT_PASS         | Password for snapshot deployment to [Artifactory](https://common.repositories.cloud.sap/ui/repos/tree/General/cap-java).                                            |
| GH_TOKEN                | Token to update the version in the `pom.xml` files. Used because the default GitHub token has insufficient rights to update the main branch without a pull request. |
| CENTRAL_REPOSITORY_USER | User for release deployment to Maven Central.                                                                                                                       |
| CENTRAL_REPOSITORY_PASS | Password for release deployment to Maven Central.                                                                                                                   |
| PGP_PUBKEY_ID           | GPG public key ID for signing release artifacts.                                                                                                                    |
| PGP_PRIVATE_KEY         | GPG private key for signing release artifacts.                                                                                                                      |
| PGP_PASSPHRASE          | GPG passphrase for signing release artifacts.                                                                                                                       |
| SONARQ_TOKEN            | Token for SonarQube code analysis.                                                                                                                                  |
| AWS*S3*\*               | AWS S3 credentials (HOST, BUCKET, REGION, ACCESS_KEY_ID, SECRET_ACCESS_KEY) for OSS integration tests.                                                              |
| AZURE\_\*               | Azure Blob credentials (CONTAINER_URI, SAS_TOKEN) for OSS integration tests.                                                                                        |
| GS\_\*                  | Google Cloud Storage credentials (BUCKET, PROJECT_ID, BASE_64_ENCODED_PRIVATE_KEY_DATA) for OSS integration tests.                                                  |

## Feature

In the project folder `cds-feature-attachments` the feature for attachments is implemented.
The feature is implemented with the following Java packages:

| Package Name    | Description                                                                                                                                                                                                                     |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `configuration` | This package includes the class which instantiates all project classes and register handler and services in the CAP Java runtime.                                                                                               |
| `handler`       | This package includes the handler for the `DraftService` and `ApplicationService`. The handler are registered for create, update, delete and read and forward calls to the `AttachmentService`.                                 |
| `service`       | This package includes the definition of the `AttachmentService`. The service is used to store and retrieve attachments. It also includes the default implementation of the `AttachmentService` handler and the malware scanner. |
| `utilities`     | This package includes helper classes used in the other packages.                                                                                                                                                                |

### CDS Model

The CDS model of the attachments is defined in the main resources folder in the `cds-feature-attachments` Java project.
According to the [CAP Java plugin concept](https://cap.cloud.sap/docs/java/building-plugins#building-plugins) the CDS
model is defined in the following folders in the `resources`:

- `cds`/`com.sap.cds`/`cds-feature-attachments`

The cds model contains two files:

- `attachments.cds`: model and aspect definition
- `attachments-annotations.cds`: UI/OData annotations for the attachments

A `index.cds` file is also included in this folder, which references the other two files.

In the model, a new annotation is introduced to mark an entity as an attachment entity:

- `@_is_media_data`

The handler for the `DraftService` and `ApplicationService` checks if the entity has this annotation, and if so,
the entity is treated as an attachment entity.

#### ETag

The field `modifiedAt` is annotated with the `@odata.etag` annotation to enable optimistic concurrency control.
This is needed to make sure that the content of the attachment is not changed during the update of the entity.
The Fiori Elements UI supports this out of the box, so no other annotations or adjustments are needed.

#### Usage of the CDS Model

See [usage section](../README.md#usage) in the main README.md file.

### Configuration

The class which instantiates all project classes and register handler and services in the CAP Java runtime is
implemented in the
`configuration` package.
It uses the `CdsRuntimeConfiguration` interface and overwrite the `services` and `eventHandlers` methods.
In the `services` method the `AttachmentService` is registered and in the `eventHandlers` method the `DraftService`
and `ApplicationService` handlers are registered.

The class `Registration` which implements this configuration is referenced in the `META-INF/services` folder
in file `com.sap.cds.services.runtime.CdsRuntimeConfiguration` to make it visible for the CAP Java runtime.

### Handlers

The following packages are the main packages for the handler implementations in the package `handler`:

- `applicationservice`: Handler for the `ApplicationService`
- `draftservice`: Handler for the `DraftService`

In the `applicationservice` package are handlers for the `create`, `update`, `delete` and `read` operations implemented.
Each operation is implemented in an own class to have a separation of concerns.

The `draftservice` package contains the handlers for the `DraftService`.
Each operation is implemented in an own class to have a separation of concerns.

The handlers check if the request contains the attachment entity by checking if the entity has the annotation
`@_is_media_data`.

This needs to be done not only for the top-level entity in the request but also for all nested entities,
as this could be the case for deep creates or deep updates.

After the handler has checked if an attachment entity is contained in the request,
the handler calls the `ModifyAttachmentEventFactory` implementation.
The implementation of this interface, `ModifyAttachmentEventFactory`, checks
based on the data included in the request which `AttachmentService` event needs to be called.

The factory returns an implementation of the `ModifyAttachmentEvent` interface.

#### Mapping CAP Events to Attachment Operations

The `ModifyAttachmentEvent` interface is a strategy object — despite its name,
it is not a CAP event or a passive value object.
Its `processEvent` method contains the logic for emitting the appropriate
`AttachmentService` event.
The name reflects that each implementation maps to one `AttachmentService` event,
but the execution logic lives inside the implementation itself.
Throughout this document, "emitting an event" means invoking the corresponding
method on the `AttachmentService` interface (e.g. `createAttachment()`), which
CAP then dispatches to the registered `@On` handler.

The `ModifyAttachmentEventFactory` selects the right strategy based on
the attachment data in the request — primarily whether `content` and `contentId`
are present or have changed.
A CAP service event (e.g. `UPDATE`) does not map one-to-one to an
`AttachmentService` event: an update could mean new content is being uploaded,
only metadata is changing, or the attachment is being removed.
The factory resolves that ambiguity.

The `ModifyAttachmentEvent` interface has the following implementations:

| Implementation                 | Description                                                                                                                                                                                                              |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `CreateAttachmentEvent`        | Emits the `CREATE_ATTACHMENT` event on the `AttachmentService`                                                                                                                                                           |
| `DoNothingAttachmentEvent`     | Used when the attachment entity is included in the request but nothing needs to be done, e.g. a field is updated which is not the `content` or `contentId` field                                                         |
| `MarkAsDeletedAttachmentEvent` | Emits the `MARK_AS_DELETED` event on the `AttachmentService`; skipped during `DRAFT_PATCH` events from the `DraftService` — in that case the mark as deleted is triggered when the draft is activated                    |
| `UpdateAttachmentEvent`        | Emits first the `MarkAsDeletedAttachmentEvent` and then the `CreateAttachmentEvent`                                                                                                                                      |

#### Draft Activate and Deep Updates

The activation of a draft is handled as a deep update in the `ApplicationService` handler.
This means that such an update could also be a delete event or a create event.

In addition, it could also be a delete event even if no attachment entity is included in the request.
If the attachment entity is not updated directly but an association to an entity that can include an attachment entity
is deleted, the delete event needs to be called for the attachment entity.

Example Entity Hierarchy:

```
root
  - item
      - attachment
```

If in this example an item is deleted which included an attachment entity the delete event needs to be called for the
attachment entity.
To be able to do this, the handler calls `AttachmentsReader` to read existing attachments from the database.
The reader is only called if an association is included in the request.

The reader uses the `AssociationCascader` to determine the associations which need to be read for the attachment
entity included in the entity hierarchy.
With the information from the cascader, the reader creates a select statement to read all attachment entities that belong to
the data in the request.

After the data are read, the update handler compares the data with the data in
the request and emits the `MARK_AS_DELETED` event for all attachments which are
not included in the request.
This is relevant when an external storage plugin is active — in that case
`MARK_AS_DELETED` triggers actual content deletion in the external store.
For the default implementation, the DB cascade already removes the attachment
rows, so this step is a no-op.

#### Content for new Draft

When the `DraftService` creates a draft from an active entity that already has
an attachment, it copies the entity data into the draft table but not the
attachment content itself — the content remains in storage, referenced by
`contentId`.
As a result, the draft row has `contentId` populated but `content` empty.

When the user activates the draft without modifying the attachment,
`UpdateAttachmentsHandler` must decide what to do with the attachment.
The `ModifyAttachmentEventFactory` makes this decision based on the data:
empty `content` alone is ambiguous — it could mean "leave the attachment alone"
or "the attachment was removed".
To resolve this, the factory checks `contentId` instead: if it is present, the
attachment still exists and `DoNothingAttachmentEvent` is returned; if it is
null, the attachment was genuinely removed and `MarkAsDeletedAttachmentEvent`
is returned.

For the default implementation, where content is stored as a blob in the
database, `contentId` would not naturally be set.
Therefore `DefaultAttachmentsServiceHandler` explicitly fills `contentId` with
the attachment's entity ID on every create — ensuring the factory can
distinguish "untouched" from "removed" during draft activation.

#### Delete

For the delete event, the handler does not call the `AttachmentService` directly; instead, an outboxed version of the `AttachmentService`
is used.
With this, the calls to the `AttachmentService` are stored in the database and only executed after the transaction is
committed.
So if the transaction had errors and needs to be rolled back, the delete event is not emitted and no rollback of the delete is needed.

More information about the outbox can be found in
the [CAP Java documentation](https://cap.cloud.sap/docs/java/outbox#outboxing-cap-service-events).
See also the [process overview](./Processes.md#delete) of the delete event.

#### Draft Keys

In some requests, e.g. the activation of a draft, the key table of this entity contains the key `IsActiveEntity`.
Unfortunately, this field has the value `false`, which is wrong in the context of the draft activation and also for reading
existing data from the database.

Because of this, this field is removed before requests to the database are executed.

#### Sibling Entity (Draft or Active)

In the draft handlers, the active entity is sometimes needed to determine the
correct attachment operation — for example, to compare the active `contentId`
against the draft `contentId` when cancelling a draft.

`DraftUtils` provides two helper methods for navigating between the two
siblings:

```java
DraftUtils.getActiveEntity(entity);  // returns the active sibling
DraftUtils.getDraftEntity(entity);   // returns the draft sibling
```

Both methods use `Drafts.SIBLING_ENTITY` from the CAP framework to navigate
to the sibling, and `DraftUtils.isDraftEntity()` to determine which direction
to navigate.
Whether an entity is a draft is determined by checking whether its qualified
name ends with the `DRAFT_TABLE_POSTFIX` constant (`_drafts`), also defined
in `DraftUtils`.

#### Readonly Fields

Some fields are defined as readonly in the CDS model because they are set by
the back end, potentially via an external system.
The following fields are readonly fields:

- `contentId`
- `status`
- `scannedAt`

Other fields which are readonly like `createdAt` can be set once a draft entity is activated.
But the fields `contentId`, `status` and `scannedAt` are readonly fields which are not allowed to be updated during the
activation of a draft entity.

Because readonly fields are deleted from the event context during the draft activate, the fields need to be stored and
added to the event context again after they are deleted, but only in case of the draft activate.

To be able to identify the draft activation, a handler for draft activation is implemented:

- `DraftActiveAttachmentsHandler`

This handler overwrites the `on` event of the draft activation and sets a flag in the `ThreadDataStorageSetter` to identify
the draft activation.

After that, the origin handler is called.

In the `ApplicationService` handler for create or update, this information is used to store the readonly fields
in a new field which is not readonly and not deleted:

- `DRAFT_READONLY_CONTEXT`

This is done before the readonly fields are deleted by using a method annotated with:

```java
@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
```

The new field is added directly in the data for the attachment entity.
In case the request is not a draft activation, the field is cleared to make sure that the field is not filled from
outside.

The method in the `ApplicationService` handler that processes the data runs at a later point to ensure that all validations are executed first.
Because of this, the method is annotated with:

```java
@HandlerOrder(HandlerOrder.LATE)
```

During the processing of the attachment entity the readonly fields are restored from the field `DRAFT_READONLY_CONTEXT`.

#### Optimistic Concurrency Control

To avoid the possibility that two update requests update the content field of the same attachment entity we use
[optimistic concurrency control](https://cap.cloud.sap/docs/java/working-with-cql/query-execution#optimistic) with an Etag for these entities.

The concurrency control is only needed for updates, as for new attachment entities there is no possibility to overwrite existing data
or data from another transaction, since new entities are always created.

### Service

Within this plugin a new technical service `AttachmentService` is implemented in package `service`.
The service is called from the handler of the `DraftService` and `ApplicationService` to store, delete, restore and
retrieve attachments.

The following table gives an overview of the events and methods of the `AttachmentService`:

| Event                        | Method in `AttachmentService` | Description                                     |
| ---------------------------- | ----------------------------- | ----------------------------------------------- |
| `CREATE_ATTACHMENT`          | `createAttachment`            | Create a new attachment with the given content. |
| `MARK_ATTACHMENT_AS_DELETED` | `markAttachmentAsDeleted`     | Called to mark an attachment as deleted.        |
| `RESTORE_ATTACHMENT`         | `restoreAttachment`           | Called to restored attachments.                 |
| `READ_ATTACHMENT`            | `readAttachment`              | Read the content of a attachment.               |

#### Service Interface

The interface for the service is `AttachmentService`.
The service is registered in the CAP Java framework, so the interface can be used in the Spring Boot context to autowire
the implementation of the service.

#### Multi-Tenancy

The feature is ready for multi-tenancy scenarios.
Tenant information does not need to be passed explicitly when emitting events
on the `AttachmentService` — CAP propagates it automatically via the
`RequestContext`, which is available on the current thread.

#### Default Implementation

In the `service.handler` package, the default handler implementation of the
`AttachmentService` is implemented.
The class `DefaultAttachmentsServiceHandler` is registered for the events of
the `AttachmentService` and implements the `@On` handler for the service.

For the default implementation, attachment content is stored as a blob in the
database by CAP's generic persistence mechanism — not by any explicit code in
`DefaultAttachmentsServiceHandler`.
This works through a two-way contract between the `AttachmentService` and the
calling handler:

1. `DefaultAttachmentsServiceHandler` sets `isInternalStored=true` on the
   create event context, signalling that content should remain in the data map.
2. The calling handler (`CreateAttachmentsHandler`) checks this flag and, if
   `true`, leaves the content stream in the data — so CAP writes it to the
   database column automatically as part of the normal `CREATE` operation.

For an external storage plugin, `isInternalStored` is left `false`, which
causes the calling handler to remove the content from the data map, preventing
CAP from writing it to the database.

The `AttachmentService` create event is emitted during the `@Before` phase,
before the entity exists in the database, so there is no way for
`DefaultAttachmentsServiceHandler` to write the content itself at that point —
the flag-based contract is the only viable approach.

For the create event, a listener is registered for the end of the transaction
to scan the content of the attachment for malware.

##### Content ID

As there is no external storage for the default implementation, the content ID is filled with the ID of the attachment
entity.
The content ID needs to be filled, as the `ModifyAttachmentEventFactory` also uses this ID to determine which event to
use.

#### Malware Scan

##### User Experience

When a user uploads an attachment, the upload succeeds immediately but the
attachment is placed in `Scanning` status.
The scan runs asynchronously after the transaction commits, so the attachment
is temporarily unavailable for download.

Once the scan completes:
- **`Clean`** — the attachment can be downloaded.
- **`Infected` or `Failed`** — the download is blocked with a "not clean" error.

If the scan has not yet completed and the user attempts to download, they
receive a "not scanned yet" error.
In that case, the plugin retriggers the scan so that the status is updated as
soon as possible.

If no malware scanner is bound, the status is set to `Clean` immediately and
downloads are never blocked.

When an attachment is created, a change set listener is registered to trigger
a malware scan after the transaction commits.
This applies regardless of the storage backend.
To see the whole process have a look in the [process description](Processes.md#malware-scan).

##### Implementation

The malware scan is implemented in package `service.handler.malware`.
There are two interfaces implemented for the malware scan:

- `AsyncMalwareScanExecutor`: to call the malware scan in an asynchronous way
- `AttachmentMalwareScanner`: to scan the content of the attachment for malware

There are two places where the malware scan is called:

1. In the change set listener registered in the `@On` handler of the `AttachmentService` create event
2. In the `ReadAttachmentsHandler`, a handler which is registered for the `ApplicationService`
   to read the content of the attachment

The interface `AsyncMalwareScanExecutor` and the `ChangeSetListener` are both implemented in
record `EndTransactionMalwareScanRunner`
to have the logic to start the malware scan in a asynchronous way in one place.

The class `DefaultAttachmentMalwareScanner` implements the interface `AttachmentMalwareScanner` and does the scan.

###### Read Data

Before the scan is executed the attachment entity data are read from the database.

As the data could be stored in the active or in the draft entity, in case the draft is activated, the data
are read from both the draft entity and the active entity.
If there is no draft entity available, the data are only read from the active entity.

The data are read with the where condition on the `contentId`.

###### Scan Content

For each attachment entity found during the selection, the content is scanned using the
`MalwareScanClient`.
The content is obtained from the `content` database column if it is non-null
(default implementation, where the blob is stored in the database).
If the column is null (external storage such as OSS),
`attachmentService.readAttachment()` is called instead,
which fetches the content from the external store via the registered `@On` handler.

The client first checks if a malware scanner is bound; if not, it returns the status `NO_SCANNER`.
In this case a warning log is written stating that no scanner is bound and that this should not be used in productive systems.

If a scanner is bound, the scanner is called and the result is mapped to the `MalwareScanResultStatus` and returned.

###### Store Scan Result

After the client returns the `MalwareScanResultStatus`, this status is mapped to the possible status values defined in
the data model.
With this, an update of the attachment entity is attempted.

###### Read Attachment calls Malware Scan

Because a draft entity could be in the phase of activation at the moment the scan is executed,
the update of the scan status will not be recognized if it happens _after_ the activation, because it targets a draft table row.
In this case the status will remain `Scanning` or `Unscanned`.

A more robust solution would ensure that attachments stuck in `Scanning` or `Unscanned` are
eventually rescanned without requiring a user download.
For now, the scan is retriggered when a read is executed for an entity with one of these status values.
Because of this, the malware scan also needs to be triggered from the `ReadAttachmentsHandler`.

##### Status

The following table gives an overview of the possible status values and the mapping to the `MalwareScanResultStatus`:

| Status Value | MalwareScanResultStatus | Description                                                                                              |
| ------------ | ----------------------- | -------------------------------------------------------------------------------------------------------- |
| `Unscanned`  | -                       | The content was not scanned.                                                                             |
| `Scanning`   | -                       | The content is scanned.                                                                                  |
| `Clean`      | `CLEAN`, `NO_SCANNER`   | The content was scanned and is clean. In case there is no scanner available the content is always clean. |
| `Infected`   | `INFECTED`, `ENCRYPTED` | The content was scanned infected or encrypted content was found.                                         |
| `Failed`     | `FAILED`                | During the scan an error occurred.                                                                       |

Only content with the status `Clean` can be downloaded from the UI.
This is validated in the `AttachmentStatusValidator`. If the content is not clean, the validator throws
an `AttachmentStatusException`.

### Texts

Default texts are delivered with the feature in [35 languages](https://github.com/cap-java/cds-feature-attachments/tree/main/cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/_i18n).
These can be overwritten by applications with custom texts or translations.

For more information see the [Model Texts](../README.md#model-texts) section in the README.md file.

## Tests

### Unit Tests

The feature has unit tests for each class.
In the `cds-feature-attachments/pom.xml`, the plugin `jacoco-maven-plugin` is used to check if every class has a unit
test.
The following settings are used for this plugin:

| Setting              | Value |
| -------------------- | ----- |
| Instruction Coverage | 95%   |
| Branch Coverage      | 95%   |
| Complexity Coverage  | 95%   |
| Class Missed Count   | 0     |

### Integration Tests

Spring Boot tests are implemented in the `integration-tests` folder.
The tests are executed during the build of the project in the GitHub Actions.

The folder `integration-tests` contains a simple Spring Boot application with the default CAP
folder structure but without UI. All the tests are done using OData V4 requests.

In the `db` folder a simple data model is implemented and in the `srv` folder services with draft enabled and without
which uses the data model from the `db` folder.

There are several tests implemented with different scenarios.
To differentiate between the scenarios profiles are used in the tests and the setup.

The following profiles are used:

- `test-handler-enabled`
- `test-handler-disabled`
- `malware-scan-enabled`

With the profile `test-handler-enabled` a handler is implemented which overwrites the default handler for the
`AttachmentService`. With this an overwrite of the default implementation is tested.

With the profile `test-handler-disabled` the default implementation of the `AttachmentService` is used.

The profile `malware-scan-enabled` is used to test the malware scan. To enable a malware scan an environment variable
is registered in the `application.yaml` of the tests if this profile is active which contains the settings for the
malware scanner.

#### Object Store Integration Tests

Integration tests for object stores (AWS S3, Azure Blob, Google Cloud Storage) are run in the CI pipeline using the
`oss` test type. These tests require cloud storage credentials to be configured as secrets (see [Secrets](#secrets)).
The tests verify that attachments can be stored and retrieved from external object stores.

The main tests are implemented in the following two packages:

- `draftservice`: Tests for the draft service
- `nondraftservice`: Tests for the application service without draft enabled

Both packages contain a base class which implements most of the tests and has abstract methods for the specific
validation
based on the scenarios.

To mock the results of the malware scan wiremock is used for the tests where the malware scanner is available.
With this positive and negative results of the malware scan can be tested.

### Coverage

Coverage data is collected per module via JaCoCo's `prepare-agent` goal. Each source module and integration test module produces its own `jacoco.exec` file during test execution.

The `coverage-report` module aggregates all coverage data and enforces thresholds. It has no source code of its own and performs the following steps during the `verify` phase:

1. **Merge** — all `.exec` files from source modules and integration tests are merged into a single `jacoco-merged.exec`.
2. **Report** — an aggregated HTML/XML/CSV report is generated via `report-aggregate`. Generated code (`com.sap.cds.feature.attachments.generated`) is excluded.
3. **Check** — coverage thresholds are enforced per module against the merged data:

| Check | Instruction | Branch | Complexity | Missed Classes |
|---|---|---|---|---|
| `cds-feature-attachments` | 95% | 95% | 95% | 0 |
| `cds-feature-attachments-oss` | 80% | 80% | 80% | 0 |
| `cds-feature-attachments-fs` | 0% (placeholder) | 0% | 0% | 4 |

The aggregated report is published to SonarQube for unified coverage visibility. The `coverage-report` module is excluded from Maven Central publishing and from the `skip-integration-tests` profile, since it depends on integration test execution data.

## Quality Tools

The following quality tools are used in the project to ensure the quality of the code and project:

| Tool                  | Definition                                                               | Description                                                                                         |
| --------------------- | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------- |
| Spotbugs              | Defined in the root `pom.xml`                                            | Static Code check for Java code working in the bytecode.                                            |
| PMD/CPD               | Defined in the root `pom.xml`                                            | Static Code check for Java code working on the source code. CPD checks the coding for duplications. |
| Maven Enforcer Plugin | Defined in the root `pom.xml`                                            | Checks if there are dependencies declared twice.                                                    |
| Jacoco                | Defined in `cds-feature-attachments/pom.xml`                             | See section [unit tests](#unit-tests).                                                              |
| Dependabot            | Config is defined in the `.github/dependabot.yml`                        | Checks for new versions of dependencies.                                                            |
| CodeQL                | Defined in `pipeline.yml`                                                | Checks for vulnerabilities in the coding. Executed as part of the CI pipeline.                      |
| SonarQube             | Defined in `.pipeline/config.yml` and `.github/actions/scan-with-sonar`  | Static code analysis and code quality metrics. Reports are available at SAP's SonarQube instance.   |
| BlackDuck             | As GitHub action defined in folder `.github/actions/scan-with-blackduck` | Scans the coding with BlackDuck for vulnerabilities. See [design](#blackduck).                      |
