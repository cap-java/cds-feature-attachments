/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link OSClient} instances from object store service bindings. Detects the
 * storage backend (AWS S3, Azure Blob Storage, Google Cloud Storage) based on the credentials in the
 * binding.
 */
public final class OSClientFactory {

  private static final Logger logger = LoggerFactory.getLogger(OSClientFactory.class);

  private OSClientFactory() {}

  /**
   * Creates an {@link OSClient} for the given service binding.
   *
   * <ul>
   *   <li>For AWS, the binding must contain a "host" with "aws", "s3", or "amazon".
   *   <li>For Azure, the binding must contain a "container_uri" with "azure" or "windows".
   *   <li>For Google, the binding must contain a valid "base64EncodedPrivateKeyData" containing
   *       "google" or "gcp".
   * </ul>
   *
   * @param binding the {@link ServiceBinding} containing credentials for the object store service
   * @param executor the {@link ExecutorService} for async operations
   * @return the appropriate {@link OSClient} implementation
   * @throws ObjectStoreServiceException if no valid object store service binding is found
   */
  public static OSClient create(ServiceBinding binding, ExecutorService executor) {
    final String host = (String) binding.getCredentials().get("host"); // AWS
    final String containerUri = (String) binding.getCredentials().get("container_uri"); // Azure
    final String base64EncodedPrivateKeyData =
        (String) binding.getCredentials().get("base64EncodedPrivateKeyData"); // GCP

    if (host != null && Stream.of("aws", "s3", "amazon").anyMatch(host::contains)) {
      logger.info("Detected AWS S3 object store from binding: {}", binding);
      return new AWSClient(binding, executor);
    } else if (containerUri != null
        && Stream.of("azure", "windows").anyMatch(containerUri::contains)) {
      logger.info("Detected Azure Blob Storage from binding: {}", binding);
      return new AzureClient(binding, executor);
    } else if (base64EncodedPrivateKeyData != null) {
      String decoded;
      try {
        decoded =
            new String(
                Base64.getDecoder().decode(base64EncodedPrivateKeyData), StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) {
        throw new ObjectStoreServiceException(
            "No valid base64EncodedPrivateKeyData found in Google service binding: %s"
                .formatted(binding),
            e);
      }
      if (Stream.of("google", "gcp").anyMatch(decoded::contains)) {
        logger.info("Detected Google Cloud Storage from binding: {}", binding);
        return new GoogleClient(binding, executor);
      } else {
        throw new ObjectStoreServiceException(
            "No valid Google service binding found in binding: %s".formatted(binding));
      }
    } else {
      throw new ObjectStoreServiceException(
          "No valid object store service found in binding: %s. Please ensure you have a valid AWS"
              + " S3, Azure Blob Storage, or Google Cloud Storage service binding."
                  .formatted(binding));
    }
  }
}
