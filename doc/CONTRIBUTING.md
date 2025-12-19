# Contributing

## Code of Conduct

All members of the project community must abide by
the [SAP Open Source Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md).
Only by respecting each other we can develop a productive, collaborative community.
Instances of abusive, harassing, or otherwise unacceptable behavior may be reported by
contacting [a project maintainer](.reuse/dep5).

## Engaging in Our Project

We use GitHub to manage reviews of pull requests.

* If you are a new contributor, see: [Steps to Contribute](#steps-to-contribute)

* Before implementing your change, create an issue that describes the problem you would like to solve or the code that
  should be enhanced. Please note that you are willing to work on that issue.

* The team will review the issue and decide whether it should be implemented as a pull request. In that case, they will
  assign the issue to you. If the team decides against picking up the issue, the team will post a comment with an
  explanation.

## Steps to Contribute

Should you wish to work on an issue, please claim it first by commenting on the GitHub issue that you want to work on.
This is to prevent duplicated efforts from other contributors on the same issue.

If you have questions about one of the issues, please comment on them, and one of the maintainers will clarify.

## Contributing Code or Documentation

You are welcome to contribute code in order to fix a bug or to implement a new feature that is logged as an issue.

The following rule governs code contributions:

* Contributions must be licensed under the [Apache 2.0 License](./LICENSE)
* Due to legal reasons, contributors will be asked to accept a Developer Certificate of Origin (DCO) when they create
  the first pull request to this project. This happens in an automated fashion during the submission process. SAP
  uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).

## Development Environment Setup

### Prerequisites

* **Java**: JDK 17 or 21 (SAPMachine, OpenJDK, or similar)
* **Maven**: 3.9.0 or higher (required for consistent build behavior with CI)
* **Git**: For version control

### Local Build Configuration

The project includes a `.mvn/maven.config` file that ensures consistent behavior between local development and CI:
- Batch mode (`-B`)
- Suppressed transfer progress (`-ntp`)
- UTF-8 encoding
- Spotless formatting checks enabled by default

### Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format style. 

**Important:** Spotless checks are now enabled by default to ensure code quality. Before committing:

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

To skip formatting checks temporarily during development:
```bash
mvn clean install -Dspotless.check.skip=true
```

### Running Tests Locally

#### Without Cloud Storage Credentials

If you don't have AWS/Azure/Google Cloud credentials configured, use the `local-testing` profile:

```bash
mvn clean verify -P local-testing
```

This skips cloud storage integration tests and uses filesystem storage targets instead.

#### With Cloud Storage Credentials

To run the full test suite including cloud storage integration tests, set these environment variables:

```bash
# AWS S3
export AWS_S3_HOST=<your-host>
export AWS_S3_BUCKET=<your-bucket>
export AWS_S3_REGION=<your-region>
export AWS_S3_ACCESS_KEY_ID=<your-access-key>
export AWS_S3_SECRET_ACCESS_KEY=<your-secret-key>

# Azure Blob Storage
export AZURE_CONTAINER_URI=<your-container-uri>
export AZURE_SAS_TOKEN=<your-sas-token>

# Google Cloud Storage
export GS_BASE_64_ENCODED_PRIVATE_KEY_DATA=<your-private-key>
export GS_BUCKET=<your-bucket>
export GS_PROJECT_ID=<your-project-id>

# Then run all tests
mvn clean verify
```

#### Running Mutation Testing

Mutation testing with pitest provides deep test quality analysis but is slower:

```bash
mvn org.pitest:pitest-maven:mutationCoverage -f cds-feature-attachments/pom.xml
```

### Common Build Commands

```bash
# Build and run unit tests
mvn clean install

# Run integration tests with current CAP version
mvn clean verify -f ./integration-tests/pom.xml

# Run integration tests with latest CAP version
mvn clean verify -f ./integration-tests/pom.xml -P latest-test-version

# Run OSS integration tests
mvn clean verify -P integration-tests-oss

# Skip tests completely
mvn clean install -DskipTests
```

### CI/CD Pipeline

The project uses an optimized CI pipeline (`.github/workflows/ci.yml`) that:
- Runs quality checks (Spotless) once
- Builds with Java 17 and runs mutation testing
- Tests compiled artifacts against Java 17 & 21
- Runs integration tests in parallel (current CAP version, latest CAP version, OSS)
- Performs SonarQube and BlackDuck security scans
- Deploys snapshots to Artifactory

For details, see [Design.md](Design.md#github-actions).

## Issues and Planning

* We use GitHub issues to track bugs and enhancement requests.

* Please provide as much context as possible when you open an issue. The information you provide must be comprehensive
  enough to reproduce that issue for the assignee.
