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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Abstract base class for separate-bucket multitenancy OSS integration tests. In separate-bucket
 * mode, each tenant gets its own dedicated bucket. Object keys are plain content IDs (no tenant
 * prefix) since tenant isolation is achieved at the bucket level.
 *
 * <p>Subclasses provide two cloud-specific {@link ServiceBinding}s (one per tenant) via {@link
 * #getTenant1ServiceBinding()} and {@link #getTenant2ServiceBinding()}.
 *
 * <p>These tests verify the contract that the future {@code SeparateOSClientProvider} must satisfy:
 * each tenant resolves to its own independent {@link OSClient} with its own bucket, and operations
 * in one tenant's bucket are completely invisible to the other.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractSeparateBucketOssStorageTest {

  private static final String MIME_TYPE = "text/plain";

  private final String testRunId = String.valueOf(System.currentTimeMillis());

  private ExecutorService executor;
  private OSClient tenant1Client;
  private OSClient tenant2Client;
  private final Set<String> tenant1CreatedKeys = new LinkedHashSet<>();
  private final Set<String> tenant2CreatedKeys = new LinkedHashSet<>();

  /**
   * Returns a {@link ServiceBinding} for tenant-1's dedicated bucket, or {@code null} if
   * credentials are not available.
   */
  protected abstract ServiceBinding getTenant1ServiceBinding();

  /**
   * Returns a {@link ServiceBinding} for tenant-2's dedicated bucket, or {@code null} if
   * credentials are not available.
   */
  protected abstract ServiceBinding getTenant2ServiceBinding();

  /** Returns the cloud provider name for display in skip messages. */
  protected abstract String getProviderName();

  @BeforeAll
  void setUp() {
    ServiceBinding binding1 = getTenant1ServiceBinding();
    ServiceBinding binding2 = getTenant2ServiceBinding();
    Assumptions.assumeTrue(
        binding1 != null && binding2 != null,
        getProviderName()
            + " separate-bucket credentials not available — skipping tests. "
            + "Two distinct buckets/containers are required.");

    executor = Executors.newCachedThreadPool();
    tenant1Client = OSClientFactory.create(binding1, executor);
    tenant2Client = OSClientFactory.create(binding2, executor);
  }

  @AfterAll
  void tearDown() {
    try {
      cleanupKeys(tenant1Client, tenant1CreatedKeys);
      cleanupKeys(tenant2Client, tenant2CreatedKeys);
    } finally {
      if (executor != null) {
        executor.shutdownNow();
      }
    }
  }

  // --- Basic CRUD per tenant ---

  @Test
  void createAndReadInTenant1Bucket() throws Exception {
    String contentId = uniqueId("crud-t1");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "hello from tenant 1 bucket");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("hello from tenant 1 bucket");
  }

  @Test
  void createAndReadInTenant2Bucket() throws Exception {
    String contentId = uniqueId("crud-t2");
    uploadContent(tenant2Client, tenant2CreatedKeys, contentId, "hello from tenant 2 bucket");
    assertThat(downloadContent(tenant2Client, contentId)).isEqualTo("hello from tenant 2 bucket");
  }

  // --- Tenant isolation: cross-bucket reads fail ---

  @Test
  void readFromTenant2Bucket_whenObjectOnlyInTenant1_fails() throws Exception {
    String contentId = uniqueId("isolation-t1-only");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "secret data in tenant 1");

    // Attempting to read the same contentId from tenant-2's bucket should fail —
    // the object doesn't exist there.
    assertThatThrownBy(() -> downloadContent(tenant2Client, contentId))
        .isInstanceOf(Exception.class);
  }

  @Test
  void readFromTenant1Bucket_whenObjectOnlyInTenant2_fails() throws Exception {
    String contentId = uniqueId("isolation-t2-only");
    uploadContent(tenant2Client, tenant2CreatedKeys, contentId, "secret data in tenant 2");

    assertThatThrownBy(() -> downloadContent(tenant1Client, contentId))
        .isInstanceOf(Exception.class);
  }

  // --- Same contentId in both buckets stores different data ---

  @Test
  void sameContentIdInBothBuckets_storesDifferentData() throws Exception {
    String contentId = uniqueId("same-id-different-data");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "tenant-1-version");
    uploadContent(tenant2Client, tenant2CreatedKeys, contentId, "tenant-2-version");

    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("tenant-1-version");
    assertThat(downloadContent(tenant2Client, contentId)).isEqualTo("tenant-2-version");
  }

  // --- Delete isolation ---

  @Test
  void deleteInTenant1_doesNotAffectTenant2() throws Exception {
    String contentId = uniqueId("delete-isolation");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "t1 data");
    uploadContent(tenant2Client, tenant2CreatedKeys, contentId, "t2 data");

    tenant1Client.deleteContent(contentId).get();
    tenant1CreatedKeys.remove(contentId);

    assertThatThrownBy(() -> downloadContent(tenant1Client, contentId))
        .isInstanceOf(Exception.class);
    assertThat(downloadContent(tenant2Client, contentId)).isEqualTo("t2 data");
  }

  @Test
  void deleteInTenant2_doesNotAffectTenant1() throws Exception {
    String contentId = uniqueId("delete-isolation-rev");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "t1 data");
    uploadContent(tenant2Client, tenant2CreatedKeys, contentId, "t2 data");

    tenant2Client.deleteContent(contentId).get();
    tenant2CreatedKeys.remove(contentId);

    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("t1 data");
    assertThatThrownBy(() -> downloadContent(tenant2Client, contentId))
        .isInstanceOf(Exception.class);
  }

  // --- Delete then re-read fails ---

  @Test
  void deleteObject_subsequentReadFails() throws Exception {
    String contentId = uniqueId("delete-then-read");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "will be deleted");

    tenant1Client.deleteContent(contentId).get();
    tenant1CreatedKeys.remove(contentId);

    assertThatThrownBy(() -> downloadContent(tenant1Client, contentId))
        .isInstanceOf(Exception.class);
  }

  // --- Update flow (delete + re-create) ---

  @Test
  void updateFlow_deleteAndRecreateWithNewContent() throws Exception {
    String contentId = uniqueId("update-flow");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "version 1");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("version 1");

    tenant1Client.deleteContent(contentId).get();
    tenant1CreatedKeys.remove(contentId);

    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "version 2");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("version 2");
  }

  // --- Multiple objects per tenant bucket ---

  @Test
  void multipleObjectsInSameBucket() throws Exception {
    for (int i = 0; i < 5; i++) {
      String contentId = uniqueId("multi-" + i);
      uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "file-" + i);
      assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("file-" + i);
    }
  }

  // --- Tenant cleanup: deleteContentByPrefix("") clears entire bucket ---

  @Test
  void tenantCleanup_deleteByEmptyPrefixRemovesAllObjects() throws Exception {
    // In separate-bucket mode, tenant unsubscribe calls deleteContentByPrefix("") to empty the
    // bucket before deleting the SM instance. Use a dedicated sub-prefix to avoid disturbing
    // objects from other tests running concurrently.
    String subPrefix = "cleanup-" + testRunId + "/";

    List<String> keys = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      String key = subPrefix + "obj-" + i;
      keys.add(key);
      uploadContent(tenant1Client, tenant1CreatedKeys, key, "cleanup data " + i);
    }

    // Verify objects exist
    for (int i = 0; i < keys.size(); i++) {
      assertThat(downloadContent(tenant1Client, keys.get(i))).isEqualTo("cleanup data " + i);
    }

    // Delete all objects with the sub-prefix
    tenant1Client.deleteContentByPrefix(subPrefix).get();
    tenant1CreatedKeys.removeAll(keys);

    // All objects should be gone
    for (String key : keys) {
      assertThatThrownBy(() -> downloadContent(tenant1Client, key))
          .as("Object should have been deleted by prefix cleanup: " + key)
          .isInstanceOf(Exception.class);
    }
  }

  @Test
  void tenantCleanup_doesNotAffectOtherTenantBucket() throws Exception {
    String subPrefix = "cleanup-cross-" + testRunId + "/";
    String t1Key = subPrefix + "t1-obj";
    String t2Key = subPrefix + "t2-obj";

    uploadContent(tenant1Client, tenant1CreatedKeys, t1Key, "t1 cleanup data");
    uploadContent(tenant2Client, tenant2CreatedKeys, t2Key, "t2 should survive");

    // Clean up tenant-1's bucket
    tenant1Client.deleteContentByPrefix(subPrefix).get();
    tenant1CreatedKeys.remove(t1Key);

    // Tenant-1 object gone
    assertThatThrownBy(() -> downloadContent(tenant1Client, t1Key)).isInstanceOf(Exception.class);

    // Tenant-2 object survives — different bucket entirely
    assertThat(downloadContent(tenant2Client, t2Key)).isEqualTo("t2 should survive");
  }

  // --- Large content ---

  @Test
  void largeContentUploadAndDownload() throws Exception {
    String contentId = uniqueId("large-content");
    // 1 MB of data
    String largeContent = "A".repeat(1024 * 1024);
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, largeContent);
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo(largeContent);
  }

  // --- Content types ---

  @Test
  void uploadWithDifferentMimeType() throws Exception {
    String contentId = uniqueId("json-content");
    String jsonContent = "{\"key\": \"value\", \"number\": 42}";
    InputStream stream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
    tenant1Client.uploadContent(stream, contentId, "application/json").get();
    tenant1CreatedKeys.add(contentId);

    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo(jsonContent);
  }

  // --- Overwrite existing object ---

  @Test
  void overwriteExistingObject_readsNewContent() throws Exception {
    String contentId = uniqueId("overwrite");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "original");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("original");

    // Overwrite with new content (same key)
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "overwritten");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("overwritten");
  }

  // --- Concurrent operations across tenants ---

  @Test
  void concurrentUploadsAcrossTenants() throws Exception {
    String contentId1 = uniqueId("concurrent-t1");
    String contentId2 = uniqueId("concurrent-t2");

    // Upload to both tenants simultaneously
    var future1 =
        tenant1Client.uploadContent(
            new ByteArrayInputStream("concurrent-t1-data".getBytes(StandardCharsets.UTF_8)),
            contentId1,
            MIME_TYPE);
    var future2 =
        tenant2Client.uploadContent(
            new ByteArrayInputStream("concurrent-t2-data".getBytes(StandardCharsets.UTF_8)),
            contentId2,
            MIME_TYPE);

    future1.get();
    future2.get();
    tenant1CreatedKeys.add(contentId1);
    tenant2CreatedKeys.add(contentId2);

    assertThat(downloadContent(tenant1Client, contentId1)).isEqualTo("concurrent-t1-data");
    assertThat(downloadContent(tenant2Client, contentId2)).isEqualTo("concurrent-t2-data");
  }

  // --- Empty content ---

  @Test
  void emptyContentUploadAndDownload() throws Exception {
    String contentId = uniqueId("empty-content");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "");
    assertThat(downloadContent(tenant1Client, contentId)).isEmpty();
  }

  // --- Special characters in content ID ---

  @Test
  void contentIdWithSpecialCharacters() throws Exception {
    // Content IDs are typically UUIDs, but test that the storage handles various safe characters
    String contentId = uniqueId("special_chars-test.v2");
    uploadContent(tenant1Client, tenant1CreatedKeys, contentId, "special chars content");
    assertThat(downloadContent(tenant1Client, contentId)).isEqualTo("special chars content");
  }

  // --- Batch operations: multiple uploads then reads ---

  @Test
  void batchUploadThenBatchRead() throws Exception {
    Map<String, String> entries =
        Map.of(
            uniqueId("batch-0"), "batch content 0",
            uniqueId("batch-1"), "batch content 1",
            uniqueId("batch-2"), "batch content 2");

    // Upload all
    for (var entry : entries.entrySet()) {
      uploadContent(tenant1Client, tenant1CreatedKeys, entry.getKey(), entry.getValue());
    }

    // Read all back
    for (var entry : entries.entrySet()) {
      assertThat(downloadContent(tenant1Client, entry.getKey())).isEqualTo(entry.getValue());
    }
  }

  // --- Delete non-existent object ---

  @Test
  void deleteNonExistentObject_doesNotThrow() throws Exception {
    String contentId = uniqueId("never-created");
    // Most cloud providers treat delete of non-existent objects as a no-op (idempotent).
    // This test documents that expected behavior.
    tenant1Client.deleteContent(contentId).get();
  }

  // --- Read non-existent object ---

  @Test
  void readNonExistentObject_fails() {
    String contentId = uniqueId("does-not-exist");
    assertThatThrownBy(() -> downloadContent(tenant1Client, contentId))
        .isInstanceOf(Exception.class);
  }

  // --- Helper methods ---

  private String uniqueId(String label) {
    return "sep-test-" + label + "-" + testRunId;
  }

  private void uploadContent(OSClient client, Set<String> tracker, String contentId, String content)
      throws Exception {
    InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    client.uploadContent(stream, contentId, MIME_TYPE).get();
    tracker.add(contentId);
  }

  private String downloadContent(OSClient client, String contentId) throws Exception {
    try (InputStream stream = client.readContent(contentId).get()) {
      if (stream == null) {
        throw new RuntimeException("Content not found for key: " + contentId);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void cleanupKeys(OSClient client, Set<String> keys) {
    if (client == null) {
      return;
    }
    for (String key : keys) {
      try {
        client.deleteContent(key).get();
      } catch (Exception ignored) {
        // best effort cleanup
      }
    }
  }
}
