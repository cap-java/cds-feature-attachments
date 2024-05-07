# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](http://semver.org/).

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## Version 1.0.2 - tbd

### Added

- added `@odata.etag` annotation for field `modifiedAt` to the Attachment entity to enable optimistic concurrency control
- added pessimistic locking for the attachment entity to prevent concurrent updates on the same attachment

### Changed

- CAP Java version was updated to 2.9.0

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
