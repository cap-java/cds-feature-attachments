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
- `DefaultAttachmentsServiceHandler` - default on-handler (stores in DB)
- Malware scanning in `service/malware/` (optional, via SAP Malware Scanning Service)

### Modification Event Factory

`ModifyAttachmentEventFactory` selects the right strategy per attachment: `CreateAttachmentEvent`, `UpdateAttachmentEvent`, `MarkAsDeletedAttachmentEvent`, or `DoNothingAttachmentEvent`.

### Configuration

`Registration` implements `CdsRuntimeConfiguration` and wires everything: services, handlers, malware scanner, outbox, CSV paths.

### CDS Model

Defined in `cds-feature-attachments/src/main/resources/cds/com.sap.cds/cds-feature-attachments/`:
- `attachments.cds` - `sap.attachments.Attachments` aspect, `MediaData` aspect, `StatusCode` enum, `ScanStates` entity
- Generated CDS4J classes: `com.sap.cds.feature.attachments.generated`

## Key Patterns

| Pattern | Where |
|---|---|
| Constructor null-check | `requireNonNull(param, "param must not be null")` in every constructor |
| Class-under-test var | `cut` in all unit tests |
| Logging | `private static final Logger logger = LoggerFactory.getLogger(Foo.class)` |
| Assertions | AssertJ (`assertThat(...)`) preferred over JUnit assertions |
| Mocking | Mockito; tests follow Arrange/Act/Assert |
| Error handling | `throw new ServiceException(ErrorStatuses.BAD_REQUEST, msg)` |
| Outbox | Persistent outbox for delete operations (reliability) |
| Thread-local | `ThreadLocalDataStorage` passes draft activation context |

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
