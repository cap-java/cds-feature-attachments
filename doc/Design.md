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

#### Draft Activate

#### Readonly Fields

### Service

#### Multi-Tenancy

#### Malware Scan


### Texts

## Integration Tests

## Mutation Tests


