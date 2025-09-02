/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleClient implements OSClient {
  private static final Logger logger = LoggerFactory.getLogger(GoogleClient.class);
  private final ExecutorService executor;
  private final Storage storage;
  private final String bucketName;

  public GoogleClient(ServiceBinding binding, ExecutorService executor) {
    this.executor = executor;
    this.bucketName = (String) binding.getCredentials().get("bucket");
    String projectId = (String) binding.getCredentials().get("projectId");
    String base64EncodedPrivateKeyData =
        (String) binding.getCredentials().get("base64EncodedPrivateKeyData");
    byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedPrivateKeyData);
    InputStream serviceAccountKeyStream = new ByteArrayInputStream(decodedBytes);
    ServiceAccountCredentials sac = null;
    try {
      sac = ServiceAccountCredentials.fromStream(serviceAccountKeyStream);
    } catch (IOException e) {
      throw new ObjectStoreServiceException("Failed to initialize Google Cloud Storage client", e);
    }
    this.storage =
        StorageOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(sac)
            .build()
            .getService();
    logger.info("Initialized client for Google Cloud Storage with binding: {}", binding);
  }

  @Override
  public Future<Void> uploadContent(
      InputStream content, String completeFileName, String contentType) {
    return executor.submit(
        () -> {
          BlobInfo blobInfo =
              BlobInfo.newBuilder(BlobId.of(bucketName, completeFileName))
                  .setContentType(contentType)
                  .build();
          // We use a writer as explained here:
          // https://cloud.google.com/java/docs/reference/google-cloud-storage/latest/com.google.cloud.storage.Storage#com_google_cloud_storage_Storage_writer_com_google_cloud_storage_BlobInfo_com_google_cloud_storage_Storage_BlobWriteOption____
          // so we can also upload large files.
          // Create the writer in a try-with-resources block to ensure it is closed properly even if
          // an exception occurs.
          try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int limit;
            while ((limit = content.read(buffer)) >= 0) {
              writer.write(ByteBuffer.wrap(buffer, 0, limit));
            }
          } catch (RuntimeException | IOException e) {
            throw new ObjectStoreServiceException(
                "Failed to upload file from Google Object Store", e);
          }
          return null;
        });
  }

  @Override
  public Future<Void> deleteContent(String completeFileName) {
    return executor.submit(
        () -> {
          // List all versions (generations) of the object to delete all of them
          // We need to do this, since versioning is enabled for Google by default
          // If we do not delete all versions, then artifacts of the attachment will
          // still remain in the storage.
          try {
            Page<Blob> blobs =
                storage.list(
                    bucketName,
                    Storage.BlobListOption.versions(true),
                    Storage.BlobListOption.prefix(completeFileName));
            for (Blob blob : blobs.iterateAll()) {
              if (blob.getName().equals(completeFileName)) {
                boolean deleted =
                    storage.delete(BlobId.of(bucketName, completeFileName, blob.getGeneration()));
                if (!deleted) {
                  throw new ObjectStoreServiceException(
                      "Failed to delete version "
                          + blob.getGeneration()
                          + " of file "
                          + completeFileName);
                }
              }
            }
          } catch (RuntimeException e) {
            throw new ObjectStoreServiceException(
                "Failed to delete file from Google Object Store", e);
          }
          return null;
        });
  }

  @Override
  public Future<InputStream> readContent(String completeFileName) {
    return executor.submit(
        () -> {
          try {
            // We read the file using a reader as explained here:
            // https://cloud.google.com/java/docs/reference/google-cloud-storage/latest/com.google.cloud.storage.Storage#com_google_cloud_storage_Storage_reader_com_google_cloud_storage_BlobId_com_google_cloud_storage_Storage_BlobSourceOption____
            // such that we can read large files.
            BlobId blobId = BlobId.of(bucketName, completeFileName);
            ReadChannel reader = storage.reader(blobId);
            return Channels.newInputStream(reader);
          } catch (RuntimeException e) {
            throw new ObjectStoreServiceException(
                "Failed to read file from Google Object Store", e);
          }
        });
  }
}
