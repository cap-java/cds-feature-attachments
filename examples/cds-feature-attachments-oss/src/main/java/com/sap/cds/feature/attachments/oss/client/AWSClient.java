package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AWSClient implements OSClient {
    public final AWSClientProvider clientProvider;

    public AWSClient(ServiceBinding binding) {
        this.clientProvider = new AWSClientProvider(binding);
    }

    public void uploadContent(InputStream content, String fileName) {
        // todo change this later to use the actualy stream and not copy it over to a byte array :)
        byte[] bytes;
        try {
            bytes = content.readAllBytes();
        } catch (Exception e) {
            System.out.println("Error reading all bytes from the input stream: " + e.getMessage());
            return;
        }
        
        InputStream newStream = new ByteArrayInputStream(bytes);
        long contentLength = bytes.length;
        S3Client s3Client = clientProvider.getS3Client();

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(clientProvider.getBucketName())
                .key(fileName)
                .contentType("text/plain")
                .build();

        s3Client.putObject(putReq, RequestBody.fromInputStream(newStream, contentLength));
    }

    public void deleteContent(String identifier) {
        System.out.println("Delete content");
    }

    public void readContent(String identifier) {
        System.out.println("Read content");
    }
}
