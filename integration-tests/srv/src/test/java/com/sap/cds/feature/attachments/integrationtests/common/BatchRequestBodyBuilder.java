package com.sap.cds.feature.attachments.integrationtests.common;

import java.util.UUID;

import org.springframework.http.HttpMethod;

public class BatchRequestBodyBuilder {

	private static final String NEW_LINE = "\r\n";

	private final String batchId;
	private String changesetId;
	private final StringBuilder builder;

	private BatchRequestBodyBuilder(String batchId) {
		this.batchId = batchId;
		this.changesetId = null;
		this.builder = new StringBuilder();
	}

	public static BatchRequestBodyBuilder getBuilder(String batchId) {
		return new BatchRequestBodyBuilder(batchId);
	}

	public String build() {
		builder.append(NEW_LINE);
		builder.append("--").append(batchId).append("--").append(NEW_LINE);
		return builder.toString();
	}

	public BatchRequestBodyBuilder startChangeset() {
		changesetId = "changeset_" + UUID.randomUUID();
		addChangesetStart();
		return this;
	}

	public BatchRequestBodyBuilder finalizeChangeset() {
		addChangesetEnd();
		changesetId = null;
		return this;
	}

	public BatchRequestBodyBuilder addGetRequest(String path) {
		addRequest(HttpMethod.GET.toString(), path);
		return this;
	}

	public BatchRequestBodyBuilder addPostRequest(String path, String payload) {
		addChangesetRequest(HttpMethod.POST.toString(), path, payload, null);
		return this;
	}

	public BatchRequestBodyBuilder addPutRequest(String path, String payload) {
		addChangesetRequest(HttpMethod.PUT.toString(), path, payload, null);
		return this;
	}

	public BatchRequestBodyBuilder addPutRequest(String path, String payload, String etag) {
		addChangesetRequest(HttpMethod.PUT.toString(), path, payload, etag);
		return this;
	}

	public BatchRequestBodyBuilder addDeleteRequest(String path, String etag) {
		addChangesetRequest(HttpMethod.DELETE.toString(), path, null, etag);
		return this;
	}

	public BatchRequestBodyBuilder addMergeRequest(String path, String payload, String etag) {
		addChangesetRequest("MERGE", path, payload, etag);
		return this;
	}

	private void addRequest(String httpMethod, String path) {
		builder.append(NEW_LINE);
		builder.append("--").append(batchId).append(NEW_LINE);

		addRequestBodyPart(httpMethod, path);

		builder.append(NEW_LINE);
	}

	private void addChangesetStart() {
		builder.append(NEW_LINE);
		builder.append("--").append(batchId).append(NEW_LINE);
		builder.append("Content-Type: multipart/mixed;boundary=").append(changesetId).append(NEW_LINE);
		builder.append(NEW_LINE);
	}

	private void addChangesetRequest(String httpMethod, String path, String payload, String etag) {
		boolean useImplicitChangeset = changesetId == null;

		if (useImplicitChangeset) {
			startChangeset();
		}

		addRequestBodyPart(httpMethod, path, payload, etag);

		if (useImplicitChangeset) {
			finalizeChangeset();
		}
	}

	private void addChangesetEnd() {
		builder.append("--").append(changesetId).append("--").append(NEW_LINE);
	}

	private void addRequestBodyPart(String httpMethod, String path) {
		builder.append("Content-Type: application/http").append(NEW_LINE);
		builder.append("Content-Transfer-Encoding: binary").append(NEW_LINE);
		builder.append(NEW_LINE);
		builder.append(httpMethod).append(" ").append(path).append(" HTTP/1.1").append(NEW_LINE);
	}

	private void addRequestBodyPart(String httpMethod, String path, String payload, String etag) {
		builder.append("--").append(changesetId).append(NEW_LINE);
		addRequestBodyPart(httpMethod, path);
		if (etag != null) {
			builder.append("If-Match: " + etag).append(NEW_LINE);
		}
		if (payload != null) {
			builder.append("Content-Type: application/json").append(NEW_LINE);
			builder.append("Content-Length: ").append(payload.length()).append(NEW_LINE);
			builder.append("Accept: application/json").append(NEW_LINE);
			builder.append(NEW_LINE);
			builder.append(payload).append(NEW_LINE);
		} else {
			builder.append(NEW_LINE);
			builder.append(NEW_LINE);
		}
	}

}
