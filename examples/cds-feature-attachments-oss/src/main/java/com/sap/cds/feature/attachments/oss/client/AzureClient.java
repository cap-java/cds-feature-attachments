package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AzureClient implements OSClient {
    private final BlobContainerClient blobContainerClient;
    private static final Logger logger = LoggerFactory.getLogger(AzureClient.class);

    public AzureClient(ServiceBinding binding) {
        // You need to extract connection string or SAS token from the binding
        this.blobContainerClient = new BlobContainerClientBuilder()
            .endpoint(binding.getCredentials().get("container_uri").toString() + "?" + binding.getCredentials().get("sas_token").toString())
            .buildClient();
    }

    @Override
    public CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        return CompletableFuture.supplyAsync(() -> {
            BlockBlobClient blockBlobClient = this.blobContainerClient.getBlobClient(completeFileName).getBlockBlobClient();
            // Todo: Change this such that large files can be uploaded
            byte[] bytes;
            try {
                bytes = content.readAllBytes();
            } catch (Exception e) {
                logger.error("Failed to read bytes from file {}: {}", completeFileName, e.getMessage());
                throw new ServiceException("Failed to read bytes from file: " + completeFileName, e);
            }
            InputStream dataStream = new ByteArrayInputStream(bytes);
            long contentLength = bytes.length;
            try {
                blockBlobClient.upload(dataStream, contentLength);
            } catch (Exception e) {
                logger.error("Failed to upload file {}: {}", completeFileName, e.getMessage());
                throw new ServiceException("Failed to upload file: " + completeFileName, e);
            }
            logger.info("Uploaded file {}", completeFileName);
            return null; // for CompletableFuture<Void>
        });
    }

    @Override
    public CompletableFuture<Void> deleteContent(String completeFileName) {
        return CompletableFuture.supplyAsync(() -> {
            BlobClient blobClient = this.blobContainerClient.getBlobClient(completeFileName);
            if (!blobClient.exists()) {
                throw new ServiceException("File not found: " + completeFileName);
            }
            try {
                blobClient.delete();
                logger.info("Deleted file {}", completeFileName);
                return null;
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
            if (!blobClient.exists()) {
                throw new ServiceException("File not found: " + completeFileName);
            }
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