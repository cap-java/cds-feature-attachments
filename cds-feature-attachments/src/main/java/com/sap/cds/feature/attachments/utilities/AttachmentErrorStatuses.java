package com.sap.cds.feature.attachments.utilities;

import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.ErrorStatuses;

public enum AttachmentErrorStatuses implements ErrorStatus {

	NOT_CLEAN("not_clean", "Attachment is not clean", ErrorStatuses.METHOD_NOT_ALLOWED),	NOT_SCANNED("not_scanned", "Attachment is not scanned, try again in a few minutes", ErrorStatuses.METHOD_NOT_ALLOWED);

	private final String code;
	private final String description;
	private final ErrorStatus httpError;

	AttachmentErrorStatuses(String code, String description, ErrorStatus httpError) {
		this.code = code;
		this.description = description;
		this.httpError = httpError;
	}

	@Override
	public String getCodeString() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getHttpStatus() {
		return httpError.getHttpStatus();
	}

}
