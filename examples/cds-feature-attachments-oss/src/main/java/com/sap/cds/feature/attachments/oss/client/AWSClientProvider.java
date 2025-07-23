package com.sap.cds.feature.attachments.oss.client;

import java.net.URI;
import java.util.Map;

import org.apache.http.client.HttpClient;

import com.sap.cds.feature.attachments.service.malware.client.HttpClientProvider;
import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpClientFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpClientFactory.DefaultHttpClientFactoryBuilder;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.security.BasicCredentials;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The default provider for getting a {@link HttpClient} for the Object Store Service.
 */
public final class AWSClientProvider implements HttpClientProvider {
    private S3Client s3Client;
    private HttpClient httpClient;
    private String bucketName;
	private static final String OS_SERVICE_LABEL = "object-store-attachments";

	/**
	 * Creates a new instance of {@link AWSClientProvider}.
	 *
	 * @param binding       the required {@link ServiceBinding} to the Malware Scan Service
	 * @param configuration the required {@link ConnectionPool} configuration
	 */
	public AWSClientProvider(ServiceBinding binding, ConnectionPool configuration) {
		this.bucketName = "x";
		String accesKeyId = "x";
		String secretAccessKey = "x";
    	Region region = Region.EU_CENTRAL_1;
		
		// Use credentials to create awsCreds
		Map<String, Object> credentials = binding.getCredentials();

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accesKeyId, secretAccessKey);
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();


		BasicCredentials basic = new BasicCredentials((String) credentials.get("username"),"password");

		String url = "url";
		URI serviceUrl = URI.create(url + "/scan").normalize();
		DefaultHttpDestination destination = DefaultHttpDestination.builder(serviceUrl)
				.name(OS_SERVICE_LABEL).basicCredentials(basic).build();

		DefaultHttpClientFactoryBuilder builder = DefaultHttpClientFactory.builder();
		builder.timeoutMilliseconds((int) configuration.getTimeout().toMillis());
		builder.maxConnectionsPerRoute(configuration.getMaxConnectionsPerRoute());
		builder.maxConnectionsTotal(configuration.getMaxConnections());
		DefaultHttpClientFactory factory = builder.build();

		this.httpClient = factory.createHttpClient(destination);
	}

	public S3Client getS3Client() {
		return this.s3Client;
	}

	public String getBucketName() {
		return this.bucketName;
	}

	public HttpClient getHttpClient() {
		return this.httpClient;
	}

}
