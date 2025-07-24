# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](http://semver.org/).

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## Version 1.1.0 - 2025-07-24

### Added

- Run integration tests also with CAP Java 4.x and @sap/cds-dk@^9 to ensure compatibility with latest available version.

### Changed

- Bumped minimum required version of CAP Java to 3.10.3.
- Avoid the generation of POJOs for `cds.outbox` and `sap.common` contexts.
- Replaced usage of generic interface `CdsData` with specific interface `Attachments` to improve type safety.

## Version 1.0.9 - 2025-06-06

### Changed

- [Introduced DraftUtils](https://github.com/cap-java/cds-feature-attachments/pull/454) to handle draft related functionality in a single place.
- Migrated deployment of `cds-feature-attachments` from [OSS](https://oss.sonatype.org) to [Central Portal](https://central.sonatype.com/).

## Version 1.0.8 - 2025-06-04

### Changed

- Simplified code base by removing classes with only a few lines of code that can be moved.
- chore: [Tidy up several pom.xml](https://github.com/cap-java/cds-feature-attachments/pull/432) files to make them more manageable
- [Improved initialization](https://github.com/cap-java/cds-feature-attachments/pull/403) of Malware Scanner Service.

## Version 1.0.7 - 2025-02-28

### Changed

- [Register draft related event handlers](https://github.com/cap-java/cds-feature-attachments/pull/386) on DraftService, if there is at least one DraftService instance available.

### Fixed

- [Fixed a potentially thrown NullPointerExcpetion](https://github.com/cap-java/cds-feature-attachments/pull/385) when adding cds-feature-attachments to a CAP Java application and the persistent outbox is not available.

## Version 1.0.6 - 2025-02-17

### Added

- [Translations](https://github.com/cap-java/cds-feature-attachments/pull/353) for all [model texts](https://github.com/cap-java/cds-feature-attachments/blob/main/README.md#model-texts).

### Changed

- [Removal of unneeded transitive dependencies](https://github.com/cap-java/cds-feature-attachments/pull/290) to avoid usage and further propagation.
- [Replaced the dependency](https://github.com/cap-java/cds-feature-attachments/pull/292) on `com.sap.cds:cds-integration-cloud-sdk` with `com.sap.cloud.sdk.cloudplatform:cloudplatform-core` to use the Cloud SDK directly.

## Version 1.0.5 - 2024-11-06

### Added

- [Added support to configure the HTTP client pool](https://github.com/cap-java/cds-feature-attachments/pull/276) to Malware Scanning Service. Supported configuration properties are:
  - `cds.attachments.malwareScanner.http.timeout`: The HTTP request timeout in seconds, defaults to 120s
  - `cds.attachments.malwareScanner.http.maxConnections`: The max. number of parallel HTTP connections to Malware Scanning Service, defaults to 20 connections

### Changed

- [Simplified logging markers](https://github.com/cap-java/cds-feature-attachments/pull/178)
- [Improved Javadoc](https://github.com/cap-java/cds-feature-attachments/pull/256)
- Updated some dependencies to latest versions.

### Fixed

- [Fixed a bug](https://github.com/cap-java/cds-feature-attachments/pull/270) that caused malware scanning to fail, if the content was stored in a HANA DB.

## Version 1.0.4 - 2024-10-22

### Added

- [Added user info](https://github.com/cap-java/cds-feature-attachments/pull/217) to deletion context.
- [Added HTML CSS](https://github.com/cap-java/cds-feature-attachments/pull/248) annotations to specify column width.

### Changed

- Updated several dependencies and Maven build plugins to latest versions.

### Fixed

- [Fixed a problem](https://github.com/cap-java/cds-feature-attachments/pull/232) with missing fileName when creating an attachment.
- [Fixed a problem](https://github.com/cap-java/cds-feature-attachments/pull/239) with deleting attachments on BTP with HANA.
- [Fixed a problem](https://github.com/cap-java/cds-feature-attachments/pull/246) with Malware scanning in multitenancy applications.

## Version 1.0.3 - 2024-09-30

### Added

- First public release on https://repo1.maven.org/maven2/com/sap/cds/

### Changed

### Fixed

## Version 1.0.2 - 2024-05-08

### Added

- added `@odata.etag` annotation for field `modifiedAt` to the Attachment entity to enable optimistic concurrency control
- added pessimistic locking for the attachment entity updates to prevent concurrent updates on the same attachment

### Changed

- CAP Java version was updated to 2.9.1

### Fixed

## Version 1.0.1 - 2024-04-29

### Added

### Changed

### Fixed

- Fixed a bug in the annotation for the status field in the MediaData entity which causes that no text was shown in the
  UI

## Version 1.0.0 - 2024-04-26

### Added

- Initial version of the project
- Data consumption model for attachments to be reusable for consumers
- New service: Attachment Service to handle attachments events
- Default handler for attachment service for storage attachments in the primary database
- Handlers for application and draft services which calls the attachment service
- Call of malware scanner if bound to consuming services
- Spring boot test for attachment service and handlers

### Changed

-
