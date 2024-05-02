# Implementation Details

## Folder Structure

The following tables shows the folder structure of the project.

| Folder                  | Description                                                                                                                 |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| .github                 | GitHub Actions definitions and the configuration of the dependabot                                                          |
| .pipeline               | Project Piper configuration, which is used in the GitHub Actions. Piper per default search in this folder for `config.yaml` |
| .reuse                  | Copyright information regarding to [Bulk License Documentation](https://reuse.software/faq/#bulk-license)                   |
| cap-notebook            | CAP notebook for creation of a test application, see [README](../cap-notebook/README.md)                                    |
| cds-feature-attachments | Implementation of the attachments feature                                                                                   |
| doc                     | Design documents and process description                                                                                    |
| integration-tests       | Spring Boot tests for the feature                                                                                           |
| LICENSES                | License description                                                                                                         |

## GitHub Actions

In folder `.github/workflows` are the GitHub Actions defined. The following table shows the actions and their purpose.

| File Name                    | Description                                                                                                                                                                                                |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pull-requests-build.yaml`   | Build the project and run unit tests, integration tests and mutation tests for Java 17 and 21 for new pull requests. Each pull request need to have green runs from this workflow to be able to be merged. |
| `main-build.yaml`            | Build the project and run unit tests, integration tests and mutation tests for Java 17 and 21 once commits are merged to the master to get an indicator if everything works with the main branch.          |
| `error-handling.yaml`        | Checks if the workflow started with `main-build.yaml` had errors and if yes, create a GitHub issue for the failing run.                                                                                    |
| `main-build-and-deploy.yaml` | Creates a new version for main, builds the project, run all tests and deploy it to maven or artifactory. See also [Build and Deploy](#build-and-deploy)                                                    |

### Pull Requests Build

The `pull-requests-build.yaml` starts a workflow to build the project and run all unit and Spring Boot tests for the
coding in the new branch including the changes in the pull request.

#### Trigger

This workflow is triggered if a new pull request is created or updated in GitHub.

### Main Build

The `main-build.yaml` starts a workflow to build the project and run all unit and Spring Boot tests for the main branch.

#### Trigger

This workflow is triggered if a new commit is pushed to the main branch.

### Error Handling

The `error-handling.yaml` starts a workflow to check if the workflow started with `main-build.yaml` had errors.
If the workflow had errors a GitHub issue is created with the error message and the failing run.

### Build and Deploy

The `main-build-and-deploy.yaml` starts a workflow to build the project, run all tests and deploy it to maven or
artifactory.
The workflow is started if a new release is created in GitHub. The tags used in the release are used as new version for
the
project.

The following steps are executed in the workflow:

1. Update the version in the `pom.xml` files. The tag used in the release is read and git commands are used to update
   the property `revision` in the parent `pom.xml` file.
2. Build the project and run all unit, integration and mutation tests.
3. Deploy the project to maven or artifactory. The deployment is done with the maven command `mvn deploy`. The
   deployment is done to the repository defined in the `pom.xml` file.

#### Trigger

This workflow is triggered if a new release is created in GitHub.
With this a new version is published to maven or artifactory once a new release is created in GitHub.

#### Repository for Deploy

In the root `pom.xml` and the `cds-feature-attachments/pom.xml` the repository for the deployment is defined.
The following code snippet shows the repository definition in the `pom.xml` file.

```xml
<distributionManagement>
    <snapshotRepository>
        <id>artifactory</id>
        <name>Artifactory_DMZ-snapshots</name>
        <url>https://common.repositories.cloud.sap/artifactory/cap-java</url>
    </snapshotRepository>
    <repository>
        <id>artifactory</id>
        <name>Artifactory_DMZ</name>
        <url>https://common.repositories.cloud.sap/artifactory/cap-java</url>
    </repository>
</distributionManagement>
```

In this example the repositories for snapshots and releases are the same but could be defined differently.
Only the root `pom.xml` and the `cds-feature-attachments/pom.xml` have defined these repositories as
only these modules shall be deployed.
If the root `pom.xml` is not deployed the usage of the `cds-feature-attachments/pom.xml` is not possible.

#### Update Version

Inside the `main-build-and-deploy.yaml` a action is called to create a new project version in the parent `pom.xml`
of the project.
The version is taken from the token of the new created release in GitHub.
The version is updated in the parent `pom.xml` file but in addition the version is written in the
`version.txt` file in the `cap-notebook` folder.
This file is used if an example project created by the CAP notebook.

##### Token for Version Update

With the current setup of the GitHub repository it is only possible to merge changes directly in the `main`
branch with an administrator account.
As the default GitHub token which is used in GitHub actions this is not possible. Because of this a
token was created and stored in the GitHub secrets under `GH_TOKEN`.
This token is used in the workflow to update the version in the `pom.xml` files.

### BlackDuck

The BlackDuck action is called in the `main-build.yaml` and `pull-requests-build.yaml` to check the project for
vulnerabilities.
The action is defined in the `../.github/actions/scan-with-blackduck/action.yaml` file.
The action uses the project piper action to call BlackDuck.

The following user group is used for the BackDuck scan:

- `CDSJAVA-OPEN-SOURCE`

The group and other settings are defined in the project piper [config](../.pipeline/config.yml) file.

### Secrets

The following secrets are stored and GitHub and used in the GitHub-actions:

| Secret Name      | Description                                                                                                                                                               |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BLACK_DUCK_TOKEN | Token for calling BlackDuck to execute BlackDuck scans.                                                                                                                   |
| DEPLOYMENT_PASS  | Password for the deployment in the [artifactory](https://common.repositories.cloud.sap/ui/repos/tree/General/cap-java).                                                   |
| DEPLOYMENT_USER  | User for the deployment in the [artifactory](https://common.repositories.cloud.sap/ui/repos/tree/General/cap-java).                                                       |
| GH_TOKEN         | Token to update the version in the `pom.xml` files. Used because the origin token created by GitHub has not enough rights to update the main branch without pull request. |

## Feature

In the project folder `cds-feature-attachments` the feature for attachments is implemented.
The feature is implemented with the following Java packages:

| Package Name    | Description                                                                                                                                                                                                                     |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
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
- `attachments-annotations.cds`: UI annotations for the attachments

A `index.cds` file is also included in this folder, which references the other two files.

In the model a new annotation is introduced to mark an entity as an attachment entity:

- `@_is_media_data`

The handler for the `DraftService` and `ApplicationService` checks if the entity has this annotation and if yes,
the entity is treated as an attachment entity.

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

### Handler

The following packages are the main packages for the handler implementation in package `handler`:

- `applicationservice`: Handler for the `ApplicationService
- `draftservice`: Handler for the `DraftService`

In the `applicationservice` package are handlers for the `create`, `update`, `delete` and `read` operations implemented.
Each operation is implemented in an own class to have a separation of concerns.

The `draftservice` package contains the handler for the `DraftService`.
Each operation is implemented in an own class to have a separation of concerns.

The handler check if the request contains the attachment entity by checking if the entity has the annotation
`@_is_media_data`.

This needs to be done not only for the top level entity in the request but also for all nested entities
as this could be the case for deep creates or deep updates.

After the handler have checked if an attachment entity is contained in the request
the handler calls the `ModifyAttachmentEventFactory` implementation.
The implementation of this interface `DefaultModifyAttachmentEventFactory` checks
based on the data included in the request which `AttachmentService`event needs to be called.

The factory returns an implementation of the `ModifyAttachmentEvent` interface.

#### Events

The `ModifyAttachmentEvent` interface has the following implementations:

| Implementation                 | Description                                                                                                                                                                                                      |
|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CreateAttachmentEvent`        | Calls the `CREATE`event of the `AttachmentService`                                                                                                                                                               |
| `DoNothingAttachmentEvent`     | Is use if the attachment entity is included in the request but nothing needs to be done in the `AttachmentService` e.g. a field is updates which is not the `content`- or `contentId` - field                    |
| `MarkAsDeletedAttachmentEvent` | Calls the `MARK_AS_DELETED` event of the `AttachmentService`, but not in case the event is a `DRAFT_PATCH`event from the `DraftService`, in this case the mark as deleted is triggered if the draft is activated |
| `UpdateAttachmentEvent`        | Calls first the `MarkAsDeletedAttachmentEvent` and after that the `CreateAttachmentEvent`                                                                                                                        |

#### Draft Activate and Deep Updates

The activation of a draft is handled as a deep update in the `ApplicationService` handler.
This means that such kind of updated could also be a delete-event or create-event.

In addition, it could also be a delete-event even if no attachment entity is included in the request.
If the attachment entity is not updated directly but an association to an entity which can include an attachment entity
is deleted
the delete-event needs to be called for the attachment entity.

Example Entity Hierarchy:

```
root 
  - item
      - attachment
```

If in this example an item is deleted which included an attachment entity the delete-event needs to be called for the
attachment entity.
To be able to do this the handler calls `DefaultAttachmentsReader` to read existing attachments from the database.
The reader is only called if an association is included in the request.

The reader uses the `DefaultAssociationCascader`to determine the associations which need to be read for the attachment
entity included
in the entity hierarchy.
With the information of the cascader the reader creates a select statement to read all attachment entity which belong to
the
data in the request.

After the data are read the update-handler compares the data with the data in the request and calls the delete-event for
all
attachments which are not included in the request.

#### Delete

For delete-events the does not call the `AttachmentService` directly but an outboxed version of the `AttachmentService`
is used and called.
With this the calls to the `AttachmentService` are stored in the database and only called after the transaction is
committed.
So, if the transaction had errors and needs to be rolled back the delete-event is not called and so no delete needs to
be rolled back.

More information about the outbox can be found in
the [CAP Java documentation](https://cap.cloud.sap/docs/java/outbox#outboxing-cap-service-events).
See also the [process overview](./Processes.md#delete) of the delete-event.

#### Draft Keys

In some requests e.g. the activation of a draft the kye table of thi entity contains the key `IsActiveEntity`.
Unfortunately, this field has the value `false` which is wrong in the context of the draft activation and also for
reading
existing data from the database.

Because of this, this field is removed before requests to the database are executed.

#### Sibling Entity (Draft or Active)

In the draft handler we need to read the active entities to determine the events.
Because in the draft handler the draft entities are used the active entities need to be read.

To get the active entity from the draft entity the following coding is used:

```java
context.getTarget().

getTargetOf("SiblingEntity");
```

With this the active entity can be determined from the draft entity.
To check if an entity is a draft entity the following coding is used:

```java
context.getTarget().

getQualifiedName().

endsWith("_drafts");
```

The constants for `SiblingEntity` and `_drafts` are defined in the `DraftConstants` class.

#### Readonly Fields

Some fields are defined as readonly fields in the CDS model because they are calculated or filled in the backend but
with
a reference to a potential external system.
The following fields are readonly fields:

- `contentId`
- `status`
- `scannedAt`

Other fields which are readonly like `createdAt` can be set once a draft entity is activated.
But the fields `contentId`, `status` and `scannedAt` are readonly fields which are not allowed to be updated during the
activation of a draft entity.

Because readonly fields are deleted from the event context during the draft activate, the fields need to be stored and
added to event context again after they are deleted, but only in case of the draft activate.

To be able to identify the draft activate process a handler for draft activate is implemented:

- `DraftActiveAttachmentsHandler`

This handler overwrites the `on` event of the draft activate and set a flag in the `ThreadDataStorageSetter` to identify
the draft activate process.

After that the origin handler is called.

In the `ApplicationService` handler for create or update this information is used to store the readonly fields
in a new field which is not readonly and not deleted:

- `DRAFT_READONLY_CONTEXT`

This is done before the readonly fields are deleted by using a method annotated with:

```java
@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
```

The new field is added directly in the data for the attachment entity.
In case the process is no activate draft process the field is cleared to make sure that the field is not filled from
outside.

The method in the `ApplicationService` handler to process the data run at a later point in time to make sure
that validations done for th data are executed.
Because of this the method is annotated with:

```java
@HandlerOrder(HandlerOrder.LATE)
```

During the processing of the attachment entity the readonly fields are restored from the field `DRAFT_READONLY_CONTEXT`.

### Service

Within this plugin a new technical service `AttachmentService` is implemented in package `service`.
The service is called from the handler of the `DraftService` and `ApplicationService` to store, delete, restore and
retrieve attachments.

The following table gives an overview of the events and methods of the `AttachmentService`:

| Event                        | Method in `AttachmentService` | Description                                     |
|------------------------------|-------------------------------|-------------------------------------------------|
| `CREATE_ATTACHMENT`          | `createAttachment`            | Create a new attachment with the given content. |
| `MARK_ATTACHMENT_AS_DELETED` | `markAttachmentAsDeleted`     | Called to mark an attachment as deleted.        |
| `RESTORE_ATTACHMENT`         | `restoreAttachment`           | Called to restored attachments.                 |
| `READ_ATTACHMENT`            | `readAttachment`              | Read the content of a attachment.               |

#### Service Interface

The interface for the service is `AttachmentService`.
The service is registered in the CAP Java framework, so the interface can be used in the Spring Boot context to autowire
the implementation of the service.

#### Default Implementation

In the `service.handler` package the default handler implementation of the `AttachmentService` is implemented.
The class `DefaultAttachmentService` is registered for the events of the `AttachmentService` and implements the
`@On` handler for the service.

Because the default implementation of the service stores the attachments in the database the `DefaultAttachmentService`
do nothing with the content of the attachment.

The handler which call the `AttachmentService` is responsible to store the content of the attachment in the database for
the default
implementation.
This is needed because the create-event is called during the `@Before` phase of the `DraftService`
and `ApplicationService`.
In this phase the attachment entity is not available in the database and so the content of the attachment can't be
stored
in the `AtachmentService` create-event default implementation.

For the create-event a listener is registered for the end of the transaction to scan the content
of the attachment for malware.

##### Internal Stored

Because the handler of the `DraftService` and `ApplicationService` need to know if the content of the attachment needs
to
be stored in the database or the storage is handled by the `AttachmentService` the create-event of
the `AttachmentService`
contains this information.
The event returns the following boolean value if the content of the attachment is stored in the database or not:

- `isInternalStored`

The default implementation sets this flag to `true`.
If there will be other implementations of the `AttachmentService` which store the content of the attachment in an
external
storage the flag must not be set.

#### Malware Scan

During the processing of the default implementation of the handler for the `AttachmentService` a malware scan is
registered.
To see the whole process have a look in the [process description](Processes.md#malware-scan).

##### Status

##### Store Scan Result

draft or no draft

#### Multi-Tenancy
### Texts

Texts are not delivered with the feature but can be added.
For more information see the following sections in the README.md file:

- [Model Texts](../README.md#model-texts)
- [Status Texts](../README.md#status-texts)
- [Error Messages](../README.md#error-messages)

## Tests

### Integration Tests

### Mutation Tests


