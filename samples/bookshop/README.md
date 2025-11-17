# Bookshop Sample - Attachments Plugin

This sample demonstrates how to use the `cds-feature-attachments` plugin in a CAP Java application. It extends the classic CAP bookshop sample to include file attachments for books.

## What This Sample Demonstrates

- Integration of the latest attachments plugin with CAP Java
- Extending existing entities with attachment capabilities
- UI integration with Fiori elements applications
- Basic attachment operations (upload, download, delete)

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher  
- Node.js 18 or higher
- npm

## Getting Started

1. **Clone and navigate to the sample**:
   ```bash
   cd samples/bookshop
   ```

2. **Install dependencies**:
   ```bash
   mvn clean install
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

## Advanced Configuration

For advanced topics like object store integration, malware scanning, and security configuration, see the [main project documentation](../../README.md).

## Troubleshooting

- **Port conflicts**: If port 8080 is in use, specify a different port: `mvn spring-boot:run -Dserver.port=8081`
- **Memory issues**: Increase JVM heap size: `export MAVEN_OPTS="-Xmx2g"`
- **File upload issues**: Check browser developer console for error messages
