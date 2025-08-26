/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class AWSClient implements OSClient {
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final String bucketName;
    private static final Logger logger = LoggerFactory.getLogger(AWSClient.class);
    private final ExecutorService executor;

    public AWSClient(ServiceBinding binding, ExecutorService executor) {
        this.executor = executor;
        Map<String, Object> credentials = binding.getCredentials();
		this.bucketName = (String) credentials.get("bucket");

		Region region = Region.of((String) credentials.get("region"));

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create((String) credentials.get("access_key_id"), (String) credentials.get("secret_access_key"));
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
		this.s3AsyncClient = S3AsyncClient.crtBuilder()
				.region(region)
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
				.build();
        logger.info("Initialized AWS S3 client");
    }

    @Override
    public Future<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        // We upload the content asynchronously, so that we can also upload large
        // files, as described here in "Using the asynchronous API":
        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/best-practices-s3-uploads.html
 
        AsyncRequestBody body = AsyncRequestBody.fromInputStream(
            content,
            null, // length = null indicates that the stream's length is unknown
            executor);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(completeFileName)
                .contentType(contentType)
                .build();

        CompletableFuture<PutObjectResponse> putFuture = this.s3AsyncClient.putObject(putRequest, body);

        return putFuture.handle((putResponse, throwable) -> {
                if (throwable != null) {
                    throw new ObjectStoreServiceException("Failed to upload file to the AWS Object Store", throwable);
                } else if (putResponse == null || !putResponse.sdkHttpResponse().isSuccessful()) {
                    String status = putResponse != null ? putResponse.sdkHttpResponse().statusText().orElse("Unknown error") : "No response received";
                    throw new ObjectStoreServiceException("Failed to upload file to the AWS Object Store, status: " + status);
                }
                return null; // for CompletableFuture<Void>
        });
    }

    @Override
    public Future<Void> deleteContent(String completeFileName) {
        return executor.submit(() -> {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(this.bucketName)
                .key(completeFileName)
                .build();
            try {
                DeleteObjectResponse delRes = this.s3Client.deleteObject(delReq);
                if (!delRes.sdkHttpResponse().isSuccessful()) {
                    String status = delRes.sdkHttpResponse().statusText().orElse("Unknown error");
                    throw new ObjectStoreServiceException("Failed to delete file from the AWS Object Store, status: " + status);
                }
            } catch (RuntimeException e) {
                throw new ObjectStoreServiceException("Failed to delete file from the AWS Object Store", e);
            }
            return null;
        });
    }

    @Override
    public Future<InputStream> readContent(String completeFileName) {
        return executor.submit(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(this.bucketName)
                .key(completeFileName)
                .build();
    
            try {
                return this.s3Client.getObject(getObjectRequest);
            } catch (RuntimeException e) {
                throw new ObjectStoreServiceException("Failed to read file from the AWS Object Store", e);
            }
        });
    }
}
