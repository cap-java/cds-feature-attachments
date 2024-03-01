CREATE TABLE unit_test_Attachment
(
    ID         NVARCHAR(36) NOT NULL,
    createdAt  TIMESTAMP_TEXT,
    createdBy  NVARCHAR(255),
    modifiedAt TIMESTAMP_TEXT,
    modifiedBy NVARCHAR(255),
    url        NVARCHAR(255),
    content    BLOB,
    mimeType   NVARCHAR(255),
    filename   NVARCHAR(255),
    note       NVARCHAR(255),
    documentId NVARCHAR(255),
    parentKey  NVARCHAR(36),
    PRIMARY KEY (ID)
);

CREATE TABLE unit_test_Roots
(
    ID    NVARCHAR(36) NOT NULL,
    title NVARCHAR(255),
    PRIMARY KEY (ID)
);

CREATE TABLE unit_test_Items
(
    ID         NVARCHAR(36) NOT NULL,
    rootId     NVARCHAR(36),
    note       NVARCHAR(255),
    events_id1 NVARCHAR(36),
    events_id2 INTEGER,
    PRIMARY KEY (ID)
);

CREATE TABLE unit_test_Events
(
    id1            NVARCHAR(36) NOT NULL,
    id2            INTEGER NOT NULL,
    content        NVARCHAR(100),
    items_ID       NVARCHAR(36),
    eventItems_id1 NVARCHAR(36),
    PRIMARY KEY (id1, id2)
);

CREATE TABLE unit_test_EventItems
(
    id1  NVARCHAR(36) NOT NULL,
    note NVARCHAR(255),
    PRIMARY KEY (id1)
);

CREATE TABLE unit_test_wrongAttachment
(
    ID       INTEGER NOT NULL,
    url      NVARCHAR(255),
    content  BLOB,
    mimeType NVARCHAR(255),
    filename NVARCHAR(255),
    PRIMARY KEY (ID)
);

CREATE VIEW unit_test_TestService_RootTable AS
SELECT Roots_0.ID,
       Roots_0.title
FROM unit_test_Roots AS Roots_0;

CREATE VIEW unit_test_TestService_Items AS
SELECT Items_0.ID,
       Items_0.rootId,
       Items_0.note,
       Items_0.events_id1,
       Items_0.events_id2
FROM unit_test_Items AS Items_0;

CREATE VIEW unit_test_TestService_Attachment AS
SELECT Attachment_0.ID,
       Attachment_0.createdAt,
       Attachment_0.createdBy,
       Attachment_0.modifiedAt,
       Attachment_0.modifiedBy,
       Attachment_0.url,
       Attachment_0.content,
       Attachment_0.mimeType,
       Attachment_0.filename,
       Attachment_0.note,
       Attachment_0.documentId,
       Attachment_0.parentKey
FROM unit_test_Attachment AS Attachment_0;

