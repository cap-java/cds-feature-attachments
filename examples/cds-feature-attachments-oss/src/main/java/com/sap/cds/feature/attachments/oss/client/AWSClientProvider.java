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
    private final S3Client s3Client;
    private final String bucketName;

	/**
	 * Creates a new instance of {@link AWSClientProvider}.
	 *
	 * @param binding       the required {@link ServiceBinding} to the Malware Scan Service
	 */
	public AWSClientProvider(ServiceBinding binding) {
		Map<String, Object> credentials = binding.getCredentials();
		System.out.println("credentials.size(): " + credentials.size());
		this.bucketName = (String) credentials.get("bucket");
		Region region = Region.of((String) credentials.get("region"));

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create((String) credentials.get("access_key_id"), (String) credentials.get("secret_access_key"));
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
