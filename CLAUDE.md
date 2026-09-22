# CLAUDE.md

CAP Java plugin providing out-of-the-box attachment storage and handling via the `Attachments` CDS aspect.

## Principles

- **Think before coding.** Read the relevant source files before proposing changes. Understand the handler chain, event flow, and CDS model before touching anything.
- **Simplicity first.** Follow existing patterns exactly. No new abstractions, helpers, or "improvements" beyond the task.
- **Surgical changes.** Change only what is needed. Don't refactor neighbors, add comments to untouched code, or introduce feature flags.
- **Goal-driven execution.** Every edit must serve the stated task. If unsure, ask.

## Project Layout

```
cds-feature-attachments/            # Core plugin (handlers, services, CDS model)
storage-targets/
  cds-feature-attachments-fs/       # File system storage (dev only)
  cds-feature-attachments-oss/      # Object store (AWS S3, Azure, GCS)
integration-tests/                  # Spring Boot integration tests
  generic/                          # Default storage tests
  mtx-local/                        # Multi-tenancy tests
  oss/                              # Object store tests
samples/bookshop/                   # Sample CAP Java app
```

Root package: `com.sap.cds.feature.attachments`

## Build & Test (Maven)

Java 17+ and Maven 3.6.3+ required.

```bash
mvn clean install                    # Full build with tests
mvn clean install -DskipTests        # Build only
mvn test                             # Unit tests
mvn verify                           # Unit + integration tests
mvn test -Dtest=FooTest              # Single test class
mvn test -Dtest=FooTest#barMethod    # Single test method
mvn spotless:apply                   # Fix formatting
mvn verify -Platest-test-version     # Test against latest CAP Java
```

**Note:** `mvn clean compile` or `mvn clean install` can occasionally fail due to a file lock. If this happens, run `mvn clean` and `mvn compile`/`mvn install` as separate commands, or just drop the `clean`.

## Code Style

- **Formatter:** Google Java Format via Spotless. Run `mvn spotless:apply` before committing.
- **License header** required on every Java file:
  ```java
  /*
   * © YEAR SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
   */
  ```
  ```
- **Imports:** static first, then non-static, both alphabetical (handled by Spotless).

## Architecture

### Two attachment models

**Composition-based**: `Attachments` is its own composition child entity, annotated `@(_is_media_data)`. Fields live in a separate DB table.

**Inline**: an `Attachment` type field on the parent entity (e.g. `avatar : Attachment`). CDS flattens it into the parent table as `avatar_content`, `avatar_contentId`, `avatar_status`, etc. Detected at runtime by scanning for elements ending in `_content` with `@(_is_media_data)`. All handlers support both models via `FieldAccessor` (`Composition` or `Inline` variant).

### Handler Layer (`handler/`)

Handlers are CAP event handlers registered for all services of a given type.

**ApplicationService handlers** (CRUD on attachment entities):
- `CreateAttachmentsHandler` / `UpdateAttachmentsHandler` / `DeleteAttachmentsHandler` / `ReadAttachmentsHandler`

**DraftService handlers** (draft lifecycle):
- `DraftActiveAttachmentsHandler` / `DraftPatchAttachmentsHandler` / `DraftCancelAttachmentsHandler`

Handler registration pattern:
```java
@ServiceName(value = "*", type = ApplicationService.class)
public class FooHandler implements EventHandler {
    @Before @HandlerOrder(HandlerOrder.EARLY)
    void processBefore(CdsXxxEventContext context) { ... }
}
```

### Service Layer (`service/`)

- `AttachmentService` - interface defining events: CREATE, READ, MARK_AS_DELETED, RESTORE
- `DefaultAttachmentsServiceHandler` - default on-handler (stores content in DB). Runs at `10 * HandlerOrder.AFTER + HandlerOrder.LATE` to guarantee it is last.
- Malware scanning in `service/malware/` (optional, via SAP Malware Scanning Service)

**Storage plugin extension point:** Plugins (`OSSAttachmentsServiceHandler`, `FSAttachmentsServiceHandler`) register `@On` handlers on `AttachmentService` with default order, intercept before the default, and call `context.setCompleted()` in `finally` to prevent the DB handler from running. On create, plugins set `context.setIsInternalStored(false)` — `CreateAttachmentEvent` uses this to return `null` to the CAP framework instead of the `InputStream`, suppressing the DB content column write.

### Modification Event Factory

`ModifyAttachmentEventFactory` selects the right strategy per attachment: `CreateAttachmentEvent`, `UpdateAttachmentEvent`, `MarkAsDeletedAttachmentEvent`, or `DoNothingAttachmentEvent`.

### Configuration

`Registration` implements `CdsRuntimeConfiguration` and wires everything: services, handlers, malware scanner, outbox, CSV paths.

### CDS Model

Defined in `cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/`:
- `attachments.cds` - `sap.attachments.Attachments` aspect, `MediaData` aspect, `StatusCode` enum, `ScanStates` entity
- Generated CDS4J classes: `com.sap.cds.feature.attachments.generated`

## Non-obvious Design Decisions

**Why creates are not outboxed, but deletes are:** The create event carries an `InputStream` (the live HTTP request body), which cannot be serialized to the DB outbox. Deletes use the outbox for two reasons: (1) **timing** — the delete must only fire after the DB transaction commits; without the outbox a delete during a transaction that later rolls back would permanently destroy content that should still exist; (2) **reliability** — if the server crashes after commit but before the delete reaches external storage, the message survives and is replayed on restart.

**Rollback cleanup via `CreationChangeSetListener`:** `CreateAttachmentEvent` registers a `CreationChangeSetListener` immediately after uploading content. If the enclosing DB transaction rolls back (`afterClose(completed=false)`), the listener fires in a **new** `ChangeSetContext` (so its own delete commits independently) and calls `markAttachmentAsDeleted` to clean up the orphaned external content. This handles the case where content upload succeeds but a later validation in the same transaction fails.

**Why two `@Before` handlers at different orders in Create/Update handlers:** `processBeforeForDraft` runs at `CHECK_CAPABILITIES` (early) to preserve readonly fields (`contentId`, `status`, `scannedAt`, `fileName`) into a `DRAFT_READONLY_CONTEXT` marker key **before** the CAP runtime strips them. `processBefore` runs at `LATE` and calls `restoreReadonlyFields` to retrieve them. If both ran at the same order, the readonly fields would already be gone.

**Read path — lazy proxy and why `BeforeReadItemsModifier` is required:** `ReadAttachmentsHandler.processAfter` returns a `LazyProxyInputStream`, not the real stream. The OData adapter sees a non-null content field and generates a `$value` download link. The actual `AttachmentService.readAttachment()` call only happens when the client follows that link and the stream is first read. `BeforeReadItemsModifier` (in `processBefore`) must inject `contentId`, `status`, and `scannedAt` into the SELECT query — without them the lazy proxy has null metadata and cannot validate status.

**Why `ThreadLocalDataStorage` instead of enriching the event context:** When CAP activates a draft (`DraftSave`), it fires internal `CdsCreate`/`CdsUpdate` events for the entities being written. Those inner events have their own `EventContext` objects with no parent reference to the outer `DraftSaveEventContext`. `DraftActiveAttachmentsHandler` sets a `ThreadLocal` flag, calls `context.proceed()` (which synchronously fires the inner handlers on the same thread), and the inner handlers read the flag. The `try/finally` in `ThreadLocalDataStorage.set()` guarantees cleanup even on exceptions, preventing thread-pool leaks.

**`MarkAsDeletedAttachmentEvent` during `DRAFT_PATCH`:** The guard `!DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())` skips the actual `markAttachmentAsDeleted` call — you can't delete external content while a draft is still open. However, the data-clearing block still runs and nullifies `contentId`/`status`/`scannedAt` (and `mimeType`/`fileName` for inline) in the draft row. The actual external delete happens at draft-cancel time via `DraftCancelAttachmentsHandler`, which compares draft vs. active contentIds.

**DraftPatch inline metadata workaround:** The CAP `DRAFT_PATCH` `@On` handler only persists `@readonly` fields added by `@Before` handlers. `mimeType` and `fileName` are not readonly, so values set by `CreateAttachmentEvent` are silently dropped. `DraftPatchAttachmentsHandler.persistInlineAttachmentMetadata` explicitly writes these fields via `PersistenceService` as a workaround.

**File size is enforced at two levels:** The `Content-Length` header is checked upfront — if present and already over the limit, the request is rejected before a single byte of the body is read. `CountingInputStream` is the actual enforcement that can't be bypassed: it counts real bytes as they flow through and throws `CONTENT_TOO_LARGE` mid-stream the moment the limit is crossed, regardless of what the header said. Without it, a client could upload an arbitrarily large file by omitting or lying about `Content-Length`. `CreateAttachmentsHandler.restoreError` catches the exception and re-wraps it with the human-readable configured size limit.

**`areKeysEmpty` in `ReadAttachmentsHandler`:** When the OData layer resolves a `$value` (raw content stream) request, CAP's internal CQN has null keys on the attachment path — `areKeysEmpty` returns `true`. For regular entity reads (metadata), keys are populated. The rescan-on-download logic therefore only triggers on actual file downloads, not list views or metadata reads. For inline attachments, `isInline()` is the equivalent trigger (inline fields are always part of the parent entity and have no own keys).

**`RESCAN_THRESHOLD = 3 days`:** Hardcoded per the SAP Malware Scanning Service FAQ recommendation. Making it configurable was not pursued.

## Key Patterns

| Pattern | Where |
|---|---|
| Constructor null-check | `requireNonNull(param, "param must not be null")` in every constructor |
| Class-under-test var | `cut` in all unit tests |
| Logging | `private static final Logger logger = LoggerFactory.getLogger(Foo.class)` |
| Assertions | AssertJ (`assertThat(...)`) preferred over JUnit assertions |
| Mocking | Mockito; tests follow Arrange/Act/Assert |
| Error handling | `throw new ServiceException(ErrorStatuses.BAD_REQUEST, msg)` |
| Outbox | Persistent outbox for **delete** operations only — ensures delete fires post-commit (not during a transaction that may roll back), and survives crashes; creates can't be outboxed because the stream isn't serializable |
| Thread-local | `ThreadLocalDataStorage` bridges `DraftSaveEventContext` → inner create/update contexts during draft activation |

## Naming

| Type | Convention | Example |
|---|---|---|
| Handler | `*Handler.java` | `ReadAttachmentsHandler` |
| Unit test | `*Test.java` | `ReadAttachmentsHandlerTest` |
| Integration test | `*IT.java` | `AWSClientIT` |
| Event context | `*EventContext.java` | `AttachmentReadEventContext` |

## Quality Gates

All enforced in CI:

- **JaCoCo:** 95% minimum (instruction, branch, complexity), 0 missed classes
- **SpotBugs:** max effort, includes tests
- **PMD:** SAP Cloud SDK rules, excludes generated code and tests
- **Spotless:** Google Java Format check

## CDS / CAP Tools

- Use `cds-mcp` tool to search CDS model definitions before building queries or modifying models.
- Use `cds-mcp` to search CAP documentation before using CAP APIs.
- Generated CDS4J classes are in `com.sap.cds.feature.attachments.generated` -- do not hand-edit.
