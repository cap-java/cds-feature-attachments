package com.sap.cds.feature.attachments.oss.client;

import java.util.Map;

import org.apache.http.client.HttpClient;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The default provider for getting a {@link HttpClient} for the Object Store Service.
 */
public final class AWSClientProvider {
    private S3Client s3Client;
    private String bucketName;
	//private static final String OS_SERVICE_LABEL = "object-store-attachments";

	/**
	 * Creates a new instance of {@link AWSClientProvider}.
	 *
	 * @param binding       the required {@link ServiceBinding} to the Malware Scan Service
	 */
	public AWSClientProvider(ServiceBinding binding) {
		Map<String, Object> credentials = binding.getCredentials();
		System.out.println("credentials.size(): " + credentials.size());
		this.bucketName = "x";
		String accesKeyId = "x";
		String secretAccessKey = "x";
    	Region region = Region.EU_CENTRAL_1;

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accesKeyId, secretAccessKey);
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
	}

	public S3Client getS3Client() {
		return this.s3Client;
	}

	public String getBucketName() {
		return this.bucketName;
	}

}
