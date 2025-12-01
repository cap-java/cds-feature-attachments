package customer.bookshop.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.catalogservice.BooksAttachments;
import cds.gen.catalogservice.BooksAttachments_;
import cds.gen.catalogservice.Books_;
import cds.gen.catalogservice.CatalogService_;

/**
 * REST Controller for managing book attachments
 * Provides RESTful endpoints alongside the existing OData service
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/attachments")
public class RestHandler {

    @Autowired
    private PersistenceService persistenceService;

    /**
     * Get all attachments for a specific book
     * GET /api/books/{bookId}/attachments
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAttachments(@PathVariable String bookId) {
        try {
            List<BooksAttachments> attachments = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.up__ID().eq(bookId))
            ).listOf(BooksAttachments.class);

            List<Map<String, Object>> response = attachments.stream().map(attachment -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", attachment.getId());
                map.put("fileName", attachment.getFileName());
                map.put("mimeType", attachment.getMimeType());
                map.put("status", attachment.getStatus());
                map.put("note", attachment.getNote());
                return map;
            }).toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get attachment metadata by ID
     * GET /api/books/{bookId}/attachments/{attachmentId}
     */
    @GetMapping(path = "/{attachmentsId}")
    public ResponseEntity<Map<String, Object>> getAttachment(
            @PathVariable String bookId,
            @PathVariable String attachmentsId) {
        try {
            Optional<BooksAttachments> attachment = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId)
                        .and(a.up__ID().eq(bookId)))
            ).first(BooksAttachments.class);

            return attachment.map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("fileName", a.getFileName());
                map.put("mimeType", a.getMimeType());
                map.put("status", a.getStatus());
                map.put("note", a.getNote());
                return ResponseEntity.ok(map);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download attachment content
     * GET /api/books/{bookId}/attachments/{attachmentId}/content
     */
    @GetMapping("/{attachmentsId}/content")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String bookId,
            @PathVariable String attachmentsId) {
        try {
            Optional<BooksAttachments> attachmentOpt = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId)
                        .and(a.up__ID().eq(bookId)))
            ).first(BooksAttachments.class);

            if (attachmentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BooksAttachments attachment = attachmentOpt.get();

            // Check if attachment is clean (security check)
            if (!"Clean".equals(attachment.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-Error-Reason", "Attachment not scanned or infected")
                    .build();
            }

            InputStream content = attachment.getContent();
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + attachment.getFileName() + "\"");

            if (attachment.getMimeType() != null) {
                headers.add(HttpHeaders.CONTENT_TYPE, attachment.getMimeType());
            } else {
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(content));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{attachmentId}/content/s")
    public ResponseEntity<Resource> downloadAttachmentS() {
        try {
        }
    }

    /**
     * Upload a new attachment
     * POST /api/books/{bookId}/attachments
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable String bookId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note) {
        try {
            // Validate book exists
            boolean bookExists = persistenceService.run(
                Select.from(Books_.class).where(b -> b.ID().eq(bookId))
            ).first().isPresent();

            if (!bookExists) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Book not found"));
            }

            // Create attachment entity
            BooksAttachments attachment = BooksAttachments.create();
            attachment.setUpId(bookId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setMimeType(file.getContentType());
            attachment.setContent(file.getInputStream());
            attachment.setStatus("Clean"); // Will be updated by malware scanner

            if (note != null && !note.trim().isEmpty()) {
                attachment.setNote(note);
            }

            // Insert attachment
            var result = persistenceService.run(
                Insert.into(BooksAttachments_.class).entry(attachment)
            );

            // Get the created attachment with generated ID
            List<BooksAttachments> createdAttachments = result.listOf(BooksAttachments.class);
            if (!createdAttachments.isEmpty()) {
                BooksAttachments created = createdAttachments.get(0);

                Map<String, Object> response = new HashMap<>();
                response.put("id", created.getId());
                response.put("fileName", created.getFileName());
                response.put("mimeType", created.getMimeType());
                response.put("status", created.getStatus());
                response.put("note", created.getNote());

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create attachment"));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload attachment: " + e.getMessage()));
        }
    }

    /**
     * Update attachment metadata
     * PATCH /api/books/{bookId}/attachments/{attachmentId}
     */
    @PatchMapping("/{attachmentsId}")
    public ResponseEntity<Map<String, Object>> updateAttachment(
            @PathVariable String bookId,
            @PathVariable String attachmentsId,
            @RequestBody Map<String, Object> updates) {
        try {
            // Verify attachment exists and belongs to the book
            Optional<BooksAttachments> existingOpt = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId)
                        .and(a.up__ID().eq(bookId)))
            ).first(BooksAttachments.class);

            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Build update statement - create the attachment to update
            BooksAttachments updateData = BooksAttachments.create();
            updateData.setId(attachmentsId);

            // Only allow updating certain fields
            if (updates.containsKey("fileName")) {
                updateData.setFileName((String) updates.get("fileName"));
            }
            if (updates.containsKey("note")) {
                updateData.setNote((String) updates.get("note"));
            }

            Update<?> updateStmt = Update.entity(BooksAttachments_.class).entry(updateData);

            // Execute update
            persistenceService.run(updateStmt);

            // Return updated attachment
            BooksAttachments updated = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId))
            ).single(BooksAttachments.class);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("fileName", updated.getFileName());
            response.put("mimeType", updated.getMimeType());
            response.put("status", updated.getStatus());
            response.put("note", updated.getNote());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update attachment: " + e.getMessage()));
        }
    }

    /**
     * Delete an attachment
     * DELETE /api/books/{bookId}/attachments/{attachmentId}
     */
    @DeleteMapping("/{attachmentsId}")
    public ResponseEntity<Map<String, Object>> deleteAttachment(
            @PathVariable String bookId,
            @PathVariable String attachmentsId) {
        try {
            // Verify attachment exists and belongs to the book
            Optional<BooksAttachments> existingOpt = persistenceService.run(
                Select.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId)
                        .and(a.up__ID().eq(bookId)))
            ).first(BooksAttachments.class);

            if (existingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Delete the attachment
            persistenceService.run(
                Delete.from(BooksAttachments_.class)
                    .where(a -> a.ID().eq(attachmentsId))
            );

            return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete attachment: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/books/{bookId}/attachments/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(@PathVariable String bookId) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Bookshop Attachments REST API");
        health.put("bookId", bookId);
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
