# Bookshop Sample - Attachments Plugin

This sample demonstrates how to use the `cds-feature-attachments` plugin in a CAP Java application. It extends the classic CAP bookshop sample to include file attachments for books.

## What This Sample Demonstrates

- Integration of the latest attachments plugin with CAP Java
- Extending existing entities with attachment capabilities
- UI integration with Fiori elements applications
- Basic attachment operations (upload, download, delete)

## Prerequisites

- Java 21 or higher
- Maven 3.9.14 or higher
- Node.js 18 or higher
- npm

## Getting Started

1. **Clone and navigate to the sample**:

   ```bash
   cd samples/bookshop
   ```

2. **Install dependencies**:

   ```bash
   mvn clean compile
   ```

3. **Run the application**:

   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**:
   - Browse Books: http://localhost:8080/browse/index.html
   - Admin Books: http://localhost:8080/admin-books/index.html

## Using Attachments

Once the application is running:

1. Navigate to the Books app (browse or admin)
2. Select any book to open its details
3. Scroll down to find the "Attachments" section
4. Use the attachment controls to:
   - Upload files by clicking the upload button
   - View uploaded files in the attachment list
   - Download files by clicking on them
   - Delete files using the delete button

## Implementation Details

### Maven Configuration

The attachments plugin is added to `srv/pom.xml`:

```xml
<dependency>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-feature-attachments</artifactId>
</dependency>
```

The `cds-maven-plugin` includes the `resolve` goal to make CDS models from dependencies available:

```xml
<plugin>
    <groupId>com.sap.cds</groupId>
    <artifactId>cds-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>cds.resolve</id>
            <goals>
                <goal>resolve</goal>
            </goals>
        </execution>
        <!-- other executions -->
    </executions>
</plugin>
```

### CDS Model Extension

The `srv/attachments.cds` file extends the Books entity with attachments:

```cds
using { sap.capire.bookshop as my } from '../db/schema';
using { sap.attachments.Attachments } from 'com.sap.cds/cds-feature-attachments';

extend my.Books with {
  attachments: Composition of many Attachments;
}
```

### UI Integration

The same file adds UI facets for both services to display attachments in the Fiori apps:

```cds
using { CatalogService as service } from '../app/services';
annotate service.Books with @(
  UI.Facets: [
    {
      $Type  : 'UI.ReferenceFacet',
      ID     : 'AttachmentsFacet',
      Label  : '{i18n>attachments}',
      Target : 'attachments/@UI.LineItem'
    }
  ]
);
```

## Storage Configuration

This sample uses the default in-memory storage, which stores attachments directly in the H2 database. For production scenarios, consider using object store backends.

## Standalone Malware Scanning

Since cds-feature-attachments 1.6.0 the malware scanner is exposed as a standalone `MalwareScannerService` that can be injected and used independently of attachment storage. This sample demonstrates the API via two REST endpoints:

- `POST /api/v1/malware/scan` — raw request body
- `POST /api/v1/malware/scan/upload` — `multipart/form-data` with a `file` field

Both return `{"status": "<CLEAN|INFECTED|ENCRYPTED|NO_SCANNER|FAILED>"}`.

### Consumer code

Injecting and calling the service in any Spring bean:

```java
import com.sap.cds.feature.attachments.service.MalwareScannerService;
import com.sap.cds.feature.attachments.service.malware.client.MalwareScanResultStatus;

@Autowired
private MalwareScannerService malwareScannerService;

public MalwareScanResultStatus scanBytes(InputStream content) {
    return malwareScannerService.scanContent(content);
}
```

See `srv/src/main/java/customer/bookshop/handlers/MalwareScanRestHandler.java` for the full controller in this sample.

### Try it locally (no binding)

Without a `malware-scanner` service binding the scanner returns `NO_SCANNER`. This still proves the service is wired correctly:

```bash
curl -u admin:admin -X POST --data-binary 'hello world' \
     http://localhost:8080/api/v1/malware/scan
# {"status":"NO_SCANNER"}

curl -u admin:admin -F file=@somefile.pdf \
     http://localhost:8080/api/v1/malware/scan/upload
# {"status":"NO_SCANNER","filename":"somefile.pdf","size":12345}
```

### Try it with a real binding (BTP hybrid mode)

To exercise `CLEAN` / `INFECTED` results, bind an SAP Malware Scanning Service instance and run in hybrid mode:

```bash
# One-time: log in to Cloud Foundry and bind the service instance
cf login
cds bind malware-scanner -2 <INSTANCE-NAME>:<SERVICE-KEY-NAME>
# creates .cdsrc-private.json with the binding

# start the app in hybrid mode so the binding is picked up
cds bind --exec mvn spring-boot:run
```

Then:

```bash
# Safe content: expected CLEAN
curl -u admin:admin -X POST --data 'hello world' \
     http://localhost:8080/api/v1/malware/scan
# {"status":"CLEAN"}

# EICAR standard antivirus test string: expected INFECTED
curl -u admin:admin -X POST \
     --data 'X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*' \
     http://localhost:8080/api/v1/malware/scan
# {"status":"INFECTED"}
```

Status codes are documented in `com.sap.cds.feature.attachments.service.malware.client.MalwareScanResultStatus`.

## Advanced Configuration

For advanced topics like object store integration, malware scanning, and security configuration, see the [main project documentation](../../README.md).

## Multi-Tenancy

This sample also supports multi-tenant mode via profiles.

### Local MTX

Run the MTX sidecar and the Java app in separate terminals:

```bash
# Terminal 1: Start the sidecar
cd mtx/sidecar
npm install
cds watch

# Terminal 2: Start the Java app with MTX profiles
cds watch --profile with-mtx-sidecar
# or: mvn spring-boot:run -Dspring-boot.run.profiles=local-mtxs
```

Mock users with tenant assignments: `admin`/`admin` (tenant t1), `erin`/`erin` (tenant t2).

### Cloud Foundry Deployment

```bash
cds add xsuaa,attachments   # if not already done
cds build --production
cf deploy gen/mta.tar
```

## Troubleshooting

- **Port conflicts**: If port 8080 is in use, specify a different port: `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"`
- **Memory issues**: Increase JVM heap size: `export MAVEN_OPTS="-Xmx2g"`
- **File upload issues**: Check browser developer console for error messages
- **View loading issues**: If your view doesn’t load, try clearing the cache and local storage for `localhost` (and the relevant port) in your browser settings or try a private/incognito tab.
