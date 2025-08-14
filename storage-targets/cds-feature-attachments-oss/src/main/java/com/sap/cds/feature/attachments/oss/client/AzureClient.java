package com.sap.cds.feature.attachments.oss.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AzureClient implements OSClient {
    private final BlobContainerClient blobContainerClient;
    private static final Logger logger = LoggerFactory.getLogger(AzureClient.class);

    public AzureClient(ServiceBinding binding) {
        this.blobContainerClient = new BlobContainerClientBuilder()
            .endpoint(binding.getCredentials().get("container_uri").toString() + "?" + binding.getCredentials().get("sas_token").toString())
            .buildClient();
        logger.info("Initialized Azure Blob Storage client");
    }

    @Override
    public CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        return CompletableFuture.runAsync(() -> {
            BlockBlobClient blockBlobClient = this.blobContainerClient.getBlobClient(completeFileName).getBlockBlobClient();
            // We upload the file here using an outputStream as documented here
            // https://javadoc.io/static/com.azure/azure-storage-blob/12.32.0-beta.1/com/azure/storage/blob/specialized/BlockBlobClient.html#getBlobOutputStream()
            // using buffers of 8KB.
            // Create the outputStream in a try-with-resources block to ensure it is closed properly.
            try (BlobOutputStream outputStream = blockBlobClient.getBlobOutputStream()){
                byte[] buffer = new byte[8192];  // 8KB buffer
                int bytesRead;
                while ((bytesRead = content.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                logger.info("Uploaded file {}", completeFileName);
            } catch (RuntimeException | IOException e) {
                logger.error("Failed to upload file {}: {}", completeFileName, e.getMessage());
                throw new ServiceException("Failed to upload file: " + completeFileName, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteContent(String completeFileName) {
        return CompletableFuture.runAsync(() -> {
            BlobClient blobClient = this.blobContainerClient.getBlobClient(completeFileName);
            try {
                blobClient.delete();
                logger.info("Deleted file {}", completeFileName);
            } catch (RuntimeException e) {
                logger.error("Failed to delete file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to delete file: " + completeFileName, e);
            }
        });
    }

    @Override
    public CompletableFuture<InputStream> readContent(String completeFileName) {
        return CompletableFuture.supplyAsync(() -> {
            BlobClient blobClient = this.blobContainerClient.getBlobClient(completeFileName);
            try {
                InputStream inputStream = blobClient.openInputStream();
                logger.info("Read file {}", completeFileName);
                return inputStream;
            } catch (RuntimeException e) {
                logger.error("Failed to read file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to read file: " + completeFileName, e);
            }
        });
    }
}
