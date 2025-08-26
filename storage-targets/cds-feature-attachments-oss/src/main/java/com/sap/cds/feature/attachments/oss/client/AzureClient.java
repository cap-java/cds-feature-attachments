/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AzureClient implements OSClient {
    private final BlobContainerClient blobContainerClient;
    private final ExecutorService executor;
    private static final Logger logger = LoggerFactory.getLogger(AzureClient.class);

    public AzureClient(ServiceBinding binding, ExecutorService executor) {
        this.executor = executor;
        this.blobContainerClient = new BlobContainerClientBuilder()
            .endpoint(binding.getCredentials().get("container_uri").toString() + "?" + binding.getCredentials().get("sas_token").toString())
            .buildClient();
        logger.info("Initialized Azure Blob Storage client");
    }

    @Override
    public Future<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        return executor.submit(() -> {
            BlockBlobClient blockBlobClient = this.blobContainerClient.getBlobClient(completeFileName).getBlockBlobClient();
            // We upload the file here using an outputStream as documented here
            // https://javadoc.io/static/com.azure/azure-storage-blob/12.32.0-beta.1/com/azure/storage/blob/specialized/BlockBlobClient.html#getBlobOutputStream()
            // using buffers of 8KB.
            // Create the outputStream in a try-with-resources block to ensure it is closed properly.
            try (BlobOutputStream outputStream = blockBlobClient.getBlobOutputStream()) {
                byte[] buffer = new byte[8192];  // 8KB buffer
                int bytesRead;
                while ((bytesRead = content.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                throw new ObjectStoreServiceException("Failed to upload file to the Azure Object Store", e);
            }
            return null;
        });
    }

    @Override
    public Future<Void> deleteContent(String completeFileName) {
        return executor.submit(() -> {
            BlobClient blobClient = this.blobContainerClient.getBlobClient(completeFileName);
            try {
                blobClient.delete();
            } catch (RuntimeException e) {
                throw new ObjectStoreServiceException("Failed to delete file from the Azure Object Store", e);
            }
            return null;
        });
    }

    @Override
    public Future<InputStream> readContent(String completeFileName) {
        return executor.submit(() -> {
            BlobClient blobClient = this.blobContainerClient.getBlobClient(completeFileName);
            try {
                return blobClient.openInputStream();
            } catch (RuntimeException e) {
                throw new ObjectStoreServiceException("Failed to read file from the Azure Object Store", e);
            }
        });
    }
}
