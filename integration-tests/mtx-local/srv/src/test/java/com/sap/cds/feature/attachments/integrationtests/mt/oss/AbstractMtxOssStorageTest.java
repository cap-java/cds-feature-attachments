/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.OSClientFactory;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Abstract base class for multitenancy OSS storage integration tests. Tests the full palette of
 * attachment operations (create, read, delete, update, tenant isolation, tenant cleanup) against a
 * real object store with shared-bucket multitenancy enabled.
 *
 * <p>Uses the {@link OSClient} directly with tenant-prefixed object keys ({@code tenantId/contentId})
 * to simulate the key structure used by {@link
 * com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler} in shared multitenancy
 * mode.
 *
 * <p>Subclasses provide the cloud-specific {@link ServiceBinding} via {@link #getServiceBinding()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMtxOssStorageTest {

  private static final String TENANT_1 = "test-tenant-1";
  private static final String TENANT_2 = "test-tenant-2";
  private static final String MIME_TYPE = "text/plain";

  private final String testRunId = String.valueOf(System.currentTimeMillis());

  private ExecutorService executor;
  private OSClient osClient;
  private final Set<String> createdObjectKeys = new LinkedHashSet<>();

  /**
   * Returns a {@link ServiceBinding} constructed from environment variables, or {@code null} if the
   * required environment variables are not set.
   */
  protected abstract ServiceBinding getServiceBinding();

  /** Returns the cloud provider name for display in skip messages. */
  protected abstract String getProviderName();

  @BeforeAll
  void setUp() {
    ServiceBinding binding = getServiceBinding();
    Assumptions.assumeTrue(
        binding != null, getProviderName() + " credentials not available — skipping tests");

    executor = Executors.newCachedThreadPool();
    osClient = OSClientFactory.create(binding, executor);
  }

  @AfterAll
  void tearDown() {
    try {
      if (osClient != null) {
        for (String key : createdObjectKeys) {
          try {
            osClient.deleteContent(key).get();
          } catch (Exception ignored) {
            // best effort cleanup
          }
        }
      }
    } finally {
      if (executor != null) {
        executor.shutdownNow();
      }
    }
  }

  @Test
  void createAndReadAttachmentForTenant1() throws Exception {
    String objectKey = objectKey(TENANT_1, uniqueId("create-t1"));
    uploadContent(objectKey, "hello from tenant 1");
    assertThat(downloadContent(objectKey)).isEqualTo("hello from tenant 1");
  }

  @Test
  void createAndReadAttachmentForTenant2() throws Exception {
    String objectKey = objectKey(TENANT_2, uniqueId("create-t2"));
    uploadContent(objectKey, "hello from tenant 2");
    assertThat(downloadContent(objectKey)).isEqualTo("hello from tenant 2");
  }

  @Test
  void tenantIsolation_tenant2CannotReadTenant1Attachment() throws Exception {
    String contentId = uniqueId("isolation");
    String t1Key = objectKey(TENANT_1, contentId);
    String t2Key = objectKey(TENANT_2, contentId);

    uploadContent(t1Key, "secret data");

    assertThatThrownBy(() -> downloadContent(t2Key)).isInstanceOf(Exception.class);
  }

  @Test
  void bothTenantsCanReadTheirOwnAttachments() throws Exception {
    String contentId = uniqueId("both-tenants");
    String t1Key = objectKey(TENANT_1, contentId);
    String t2Key = objectKey(TENANT_2, contentId);

    uploadContent(t1Key, "data for tenant 1");
    uploadContent(t2Key, "data for tenant 2");

    assertThat(downloadContent(t1Key)).isEqualTo("data for tenant 1");
    assertThat(downloadContent(t2Key)).isEqualTo("data for tenant 2");
  }

  @Test
  void deleteAttachmentForTenant1_tenant2Unaffected() throws Exception {
    String contentId = uniqueId("delete-isolation");
    String t1Key = objectKey(TENANT_1, contentId);
    String t2Key = objectKey(TENANT_2, contentId);

    uploadContent(t1Key, "tenant 1 data");
    uploadContent(t2Key, "tenant 2 data");

    osClient.deleteContent(t1Key).get();
    createdObjectKeys.remove(t1Key);

    assertThatThrownBy(() -> downloadContent(t1Key)).isInstanceOf(Exception.class);
    assertThat(downloadContent(t2Key)).isEqualTo("tenant 2 data");
  }

  @Test
  void deleteAttachment_subsequentReadFails() throws Exception {
    String objectKey = objectKey(TENANT_1, uniqueId("delete-read"));

    uploadContent(objectKey, "to be deleted");

    osClient.deleteContent(objectKey).get();
    createdObjectKeys.remove(objectKey);

    assertThatThrownBy(() -> downloadContent(objectKey)).isInstanceOf(Exception.class);
  }

  @Test
  void updateFlow_createReadDeleteCreateRead() throws Exception {
    String objectKey = objectKey(TENANT_1, uniqueId("update-flow"));

    uploadContent(objectKey, "original content");
    assertThat(downloadContent(objectKey)).isEqualTo("original content");

    osClient.deleteContent(objectKey).get();
    createdObjectKeys.remove(objectKey);

    String newObjectKey = objectKey(TENANT_1, uniqueId("update-flow-v2"));
    uploadContent(newObjectKey, "updated content");

    assertThatThrownBy(() -> downloadContent(objectKey)).isInstanceOf(Exception.class);
    assertThat(downloadContent(newObjectKey)).isEqualTo("updated content");
  }

  @Test
  void multipleAttachmentsPerTenant() throws Exception {
    List<String> objectKeys = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      String objectKey = objectKey(TENANT_1, uniqueId("multi-" + i));
      objectKeys.add(objectKey);
      uploadContent(objectKey, "file " + i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(downloadContent(objectKeys.get(i))).isEqualTo("file " + i);
    }
  }

  @Test
  void tenantCleanup_deleteByPrefixRemovesAllTenantObjects() throws Exception {
    List<String> t1Keys = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      String objectKey = objectKey(TENANT_1, uniqueId("cleanup-t1-" + i));
      t1Keys.add(objectKey);
      uploadContent(objectKey, "t1 cleanup data " + i);
    }

    String t2Key = objectKey(TENANT_2, uniqueId("cleanup-t2"));
    uploadContent(t2Key, "t2 data survives cleanup");

    osClient.deleteContentByPrefix(TENANT_1 + "/").get();
    createdObjectKeys.removeAll(t1Keys);

    for (String key : t1Keys) {
      assertThatThrownBy(() -> downloadContent(key))
          .as("Tenant-1 object should have been deleted by cleanup: " + key)
          .isInstanceOf(Exception.class);
    }

    assertThat(downloadContent(t2Key)).isEqualTo("t2 data survives cleanup");
  }

  // --- Helper methods ---

  private String uniqueId(String label) {
    return "mtx-test-" + label + "-" + testRunId;
  }

  private static String objectKey(String tenant, String contentId) {
    return tenant + "/" + contentId;
  }

  private void uploadContent(String objectKey, String content) throws Exception {
    InputStream stream = new ByteArrayInputStream(content.getBytes());
    osClient.uploadContent(stream, objectKey, MIME_TYPE).get();
    createdObjectKeys.add(objectKey);
  }

  private String downloadContent(String objectKey) throws Exception {
    try (InputStream stream = osClient.readContent(objectKey).get()) {
      if (stream == null) {
        throw new RuntimeException("Content not found for key: " + objectKey);
      }
      return new String(stream.readAllBytes());
    }
  }
}
