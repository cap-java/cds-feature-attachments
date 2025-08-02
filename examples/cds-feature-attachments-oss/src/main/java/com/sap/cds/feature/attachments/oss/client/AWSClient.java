package com.sap.cds.feature.attachments.oss.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class AWSClient implements OSClient {
    private final AWSClientProvider clientProvider;
	private static final Logger logger = LoggerFactory.getLogger(AWSClient.class);

    public AWSClient(ServiceBinding binding) {
        this.clientProvider = new AWSClientProvider(binding);
    }

    @Override
    public CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        // We upload the content asynchronously, sucht that we can also upload large
        // files, as described here in "Using the asynchronous API":
        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/best-practices-s3-uploads.html
 
        S3AsyncClient s3AsyncClient = clientProvider.getS3AsyncClient();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        AsyncRequestBody body = AsyncRequestBody.fromInputStream(
            content,
            null, // length = null indicates that the stream's length is unknown
            executor);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(clientProvider.getBucketName())
                .key(completeFileName)
                .contentType(contentType)
                .build();

        CompletableFuture<PutObjectResponse> putFuture = s3AsyncClient.putObject(putRequest, body);

        return putFuture.handle((putResponse, throwable) -> {
                try {
                    if (throwable != null) {
                        logger.error("Failed to upload file {}: {}", completeFileName, throwable.getMessage());
                        throw new ServiceException("Failed to upload file: " + completeFileName, throwable);
                    } else if (putResponse == null || !putResponse.sdkHttpResponse().isSuccessful()) {
                        String status = putResponse != null ? putResponse.sdkHttpResponse().statusText().orElse("Unknown error") : "No response received";
                        logger.error("Failed to upload file {}: {}", completeFileName, status);
                        throw new ServiceException("Failed to upload file: " + completeFileName + ", status: " + status);
                    } else {
                        logger.info("Uploaded file {}", completeFileName);
                    }
                } finally {
                    executor.shutdown();
                    try {
                        content.close();
                    } catch (IOException e) {
                        logger.error("Failed to close input stream: {}", e.getMessage());
                    }
                }
                return null; // for CompletableFuture<Void>
        });
    }

    @Override
    public CompletableFuture<Void> deleteContent(String completeFileName) {
        return CompletableFuture.runAsync(() -> {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(clientProvider.getBucketName())
                .key(completeFileName)
                .build();
            try {
                DeleteObjectResponse delRes = clientProvider.getS3Client().deleteObject(delReq);
                if (delRes.sdkHttpResponse().isSuccessful()) {
                    logger.info("Deleted file {} from bucket {}", completeFileName, clientProvider.getBucketName());
                } else {
                    String status = delRes.sdkHttpResponse().statusText().orElse("Unknown error");
                    logger.error("Failed to delete file {}: {}", completeFileName, status);
                    throw new ServiceException("Failed to delete file: " + completeFileName + ", status: " + status);
                }
            } catch (RuntimeException e) {
                logger.error("Failed to delete file {}: {}", completeFileName, e.getMessage());
                throw new ServiceException("Failed to delete file: " + completeFileName, e);
            }
        });
    }

    @Override
    public CompletableFuture<InputStream> readContent(String completeFileName) {
        return CompletableFuture.supplyAsync(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(clientProvider.getBucketName())
                .key(completeFileName)
                .build();
    
            try {
                InputStream inputStream = clientProvider.getS3Client().getObject(getObjectRequest);
                logger.info("Successfully fetched file {}", completeFileName);
                return inputStream;
            } catch (RuntimeException e) {
                logger.error("Error while reading file {}: {}", completeFileName, e.getMessage(), e);
                throw new ServiceException("Failed to read file: " + completeFileName, e);
            }
        });
    }
}