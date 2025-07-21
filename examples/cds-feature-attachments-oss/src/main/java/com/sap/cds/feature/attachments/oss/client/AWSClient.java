package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AWSClient implements OSClient {
    // Todo: Possibly remove class AWSClientProvider and create client in constructor here
    private final AWSClientProvider clientProvider;
	private static final Logger logger = LoggerFactory.getLogger(AWSClient.class);

    public AWSClient(ServiceBinding binding) {
        this.clientProvider = new AWSClientProvider(binding);
    }

    public void uploadContent(InputStream content, String completeFileName) {
        // todo change this later to use the actual stream and not copy it over to a byte array :)
        byte[] bytes;
        try {
            bytes = content.readAllBytes();
        } catch (Exception e) {
            System.out.println("Error reading all bytes from the input stream: " + e.getMessage());
            return;
        }
        
        InputStream newStream = new ByteArrayInputStream(bytes);
        long contentLength = bytes.length;

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(clientProvider.getBucketName())
                .key(completeFileName)
                //todo: mimetype!
                .contentType("text/plain")
                .build();

        clientProvider.getS3Client().putObject(putReq, RequestBody.fromInputStream(newStream, contentLength));
    }

    public void deleteContent(String completeFileName) {
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
            .bucket(clientProvider.getBucketName())
            .key(completeFileName)
            .build();
        clientProvider.getS3Client().deleteObject(delReq);
    }

    public InputStream readContent(String completeFileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(clientProvider.getBucketName())
			.key(completeFileName)
			.build();

        return clientProvider.getS3Client().getObject(getObjectRequest);
    }
}
