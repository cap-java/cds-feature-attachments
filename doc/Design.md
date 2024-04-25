# Implementation Details

## Folder Structure

The following tables shows the folder structure of the project.

| Folder                  | Description                                                                                                                 |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| .github                 | GitHub Actions definitions and the configuration of the dependabot                                                          |
| .pipeline               | Project Piper configuration, which is used in the GitHub Actions. Piper per default search in this folder for `config.yaml` |
| .reuse                  | Copyright information regarding to [Bulk License Documentation](https://reuse.software/faq/#bulk-license)                   |
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

#### Token for Version Update

With the current setup of the GitHub repository it is only possible to merge changes directly in the `main`
branch with an administrator account.
As the default GitHub token which is used in GitHub actions this is not possible. Because of this a
token was created and stored in the GitHub secrets under `GH_TOKEN`.
This token is used in the workflow to update the version in the `pom.xml` files.

### BlackDuck

### Secrets

## Feature

### CDS Model

### Configuration

### Handler

#### Events

### Service

#### Multi-Tenancy

#### Malware Scan

### Readonly Fields

### Texts

## Integration Tests


