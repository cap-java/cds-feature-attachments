# Architecture Decision Record

## Single Attachment Field: `type Attachment` vs. `Composition of one`

| .            | .                                               |
|--------------|-------------------------------------------------|
| Date         | 2026-08-12                                      |
| Version      | V0.1                                            |
| Status       | Final                                           |
| Acceptance   | Accepted                                        |
| Contributors | Buse Halis                                      |
| Reviewers    | Lisa Nebel, Marvin Lindner, Eric Peairs         |

**Version History**

| Version | Date       | Changes         |
|---------|------------|-----------------|
| V0.1    | 2026-08-12 | Initial version |

## Summary

The `cds-feature-attachments` plugin needed to support single inline attachment fields on parent entities (e.g. a profile picture or avatar). Two approaches were considered: a `Composition of one` child entity and a flattened `type Attachment`. The `type Attachment` approach was chosen because it integrates with Fiori Elements drafts out of the box and keeps all implementation complexity inside the plugin.

## Context

The plugin already provides the `Attachments` aspect for composition-based file attachments (one-to-many). A common use case is attaching a single file directly to a parent entity, for example a user profile picture. The question was how to model and implement this single-attachment case in a way that works seamlessly with CAP's draft lifecycle and Fiori Elements, without requiring custom handlers from application developers.

## Key Assumptions and Boundary Conditions

- The plugin targets CAP Java applications, typically using Fiori Elements for the UI layer.
- Draft support is a core requirement. The plugin must work with CAP's draft lifecycle out of the box.
- Implementation complexity must stay inside the plugin and must not leak to the application developer.

## Solutions Considered

**Option 1: `Composition of one`**

A dedicated child entity holds the attachment, referenced from the parent via a `Composition of one`.

- ✅ Conceptually clean, follows standard CAP composition patterns
- ❌ Fiori Elements does not support `Composition of one` with drafts out of the box. A custom handler would be required just to make the basic creation flow work (see [CAP CDL – Compositions](https://cap.cloud.sap/docs/cds/cdl#compositions))
- ❌ When PATCHing a parent entity with a `Composition of one` child, CAP does not handle the foreign key automatically. A custom handler would be needed to wire up the parent–child relationship on every upload

**Option 2: `type Attachment` (flattened inline fields)**

Fields from the `MediaData` aspect are flattened into the parent entity with a prefix (e.g. `avatar_content`, `avatar_contentId`). The plugin handles all prefix-based field resolution internally.

- ✅ Works with Fiori Elements drafts out of the box — no custom handler needed
- ✅ Application developer only declares `avatar : Attachment` — no FK wiring or lifecycle handling required
- ❌ More complex to implement inside the plugin (prefix-based handling for reads, writes, draft operations, and malware scanning)

## Decision

`type Attachment` (Option 2) was chosen. Although the implementation inside the plugin is more complex due to prefix handling, all that complexity is fully encapsulated. Application developers declare a single field of type `Attachment` on their entity and get draft support, malware scanning, and file lifecycle management without writing any custom handlers.

The `Composition of one` approach would have shifted the complexity to every application developer using the plugin, which is contrary to the plugin's core purpose.

This decision was implemented in [PR #768](https://github.com/cap-java/cds-feature-attachments/pull/768).
