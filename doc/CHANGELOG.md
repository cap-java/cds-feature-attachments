# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](http://semver.org/).

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## Version 1.0.0 - tbd

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
