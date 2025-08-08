package com.sap.cds.feature.attachments.oss.client;

import java.util.Map;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

//import jakarta.annotation.PreDestroy;

/**
 * The default provider for getting a {@link HttpClient} for the Object Store Service.
 */
public final class AWSClientProvider {
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final String bucketName;

	/**
	 * Creates a new instance of {@link AWSClientProvider}.
	 *
	 * @param binding       the required {@link ServiceBinding} to the Malware Scan Service
	 */
	public AWSClientProvider(ServiceBinding binding) {
		// The map "credentials" contains the info from the ServiceBinding-JSON that
		// can be accesssed via the SAP BTP Cockpit.
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
	}

	public S3Client getS3Client() {
		return this.s3Client;
	}

	public S3AsyncClient getS3AsyncClient() {
		return this.s3AsyncClient;
	}

	public String getBucketName() {
		return this.bucketName;
	}

}
