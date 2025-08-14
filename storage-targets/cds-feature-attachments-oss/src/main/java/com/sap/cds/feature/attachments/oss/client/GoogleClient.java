package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;


public class GoogleClient implements OSClient {
    private static final Logger logger = LoggerFactory.getLogger(GoogleClient.class);

    private final Storage storage;
    private final String bucketName;

    public GoogleClient(ServiceBinding binding) {
        // Example: get credentials and bucket from binding
        this.bucketName = (String) binding.getCredentials().get("bucket");
        String projectId = (String) binding.getCredentials().get("projectId");
        String base64EncodedPrivateKeyData = (String) binding.getCredentials().get("base64EncodedPrivateKeyData");
        byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedPrivateKeyData);
        InputStream serviceAccountKeyStream = new ByteArrayInputStream(decodedBytes);
        ServiceAccountCredentials sac = null;
        try {
            sac = ServiceAccountCredentials.fromStream(serviceAccountKeyStream);
        } catch (IOException e) {
            logger.error("Could not initialize Google Cloud Storage client: {}", e.getMessage(), e);
            throw new ServiceException("Failed to initialize Google Cloud Storage client", e);
        }
        this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(sac)
                .build()
                .getService();  
        logger.info("Initialized client for Google Cloud Storage with binding: {}", binding);
    }

    @Override
    public CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        return CompletableFuture.runAsync(() -> {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, completeFileName))
                    .setContentType(contentType)
                    .build();
            // We use a writer as explained here:
            // https://cloud.google.com/java/docs/reference/google-cloud-storage/latest/com.google.cloud.storage.Storage#com_google_cloud_storage_Storage_writer_com_google_cloud_storage_BlobInfo_com_google_cloud_storage_Storage_BlobWriteOption____
            // so we can also upload large files.
            try (WriteChannel writer = storage.writer(blobInfo)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int limit;
                while ((limit = content.read(buffer)) >= 0) {
                    writer.write(ByteBuffer.wrap(buffer, 0, limit));
                }
                logger.info("Uploaded file {}", completeFileName);
            } catch (RuntimeException | IOException e) {
                logger.error("Failed to upload file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to upload file: " + completeFileName, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteContent(String completeFileName) {
        return CompletableFuture.runAsync(() -> {
            try {
                // List all versions (generations) of the object to delete all of them
                // We need to do this, since versioning is enabled for Google by default
                // If we do not delete all versions, then artifacts of the attachment will
                // still remain in the storage.
                Page<Blob> blobs = storage.list(
                    bucketName,
                    Storage.BlobListOption.versions(true),
                    Storage.BlobListOption.prefix(completeFileName)
                );
                boolean anyDeleted = false;
                for (Blob blob : blobs.iterateAll()) {
                    if (blob.getName().equals(completeFileName)) {
                        boolean deleted = storage.delete(BlobId.of(bucketName, completeFileName, blob.getGeneration()));
                        if (deleted) {
                            logger.info("Deleted version {} of file {}", blob.getGeneration(), completeFileName);
                            anyDeleted = true;
                        } else {
                            logger.warn("Failed to delete version {} of file {}", blob.getGeneration(), completeFileName);
                        }
                    }
                }
                if (!anyDeleted) {
                    logger.warn("No versions found to delete for file {}", completeFileName);
                }
            } catch (RuntimeException e) {
                logger.error("Failed to delete file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to delete file: " + completeFileName, e);
            }
        });
    }

    @Override
    public CompletableFuture<InputStream> readContent(String completeFileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // We read the file using a reader as explained here:
                // https://cloud.google.com/java/docs/reference/google-cloud-storage/latest/com.google.cloud.storage.Storage#com_google_cloud_storage_Storage_reader_com_google_cloud_storage_BlobId_com_google_cloud_storage_Storage_BlobSourceOption____
                // such that we can read large files.
                BlobId blobId = BlobId.of(bucketName, completeFileName);
                if (storage.get(blobId) == null) {
                    logger.error("File {} not found for reading", completeFileName);
                    // We could throw an exception here, but since we log the error, we return an empty stream.
                    return new ByteArrayInputStream(new byte[0]);
                }
                ReadChannel reader = storage.reader(blobId);
                return Channels.newInputStream(reader);
            } catch (RuntimeException e) {
                logger.error("Failed to read file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to read file: " + completeFileName, e);
            }
        });
    }
}
