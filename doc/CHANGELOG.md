# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](http://semver.org/).

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## Version 1.0.5 - tbd.

### Added

### Changed

- Simplified logging markers
- [Improved Javadoc](https://github.com/cap-java/cds-feature-attachments/pull/256)
- Updated dependencies and Maven build plugins to latest versions.

### Fixed

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
