package com.sap.cds.feature.attachments.service.model.service;

import com.sap.cds.services.request.UserInfo;

/**
	* The class {@link MarkAsDeletedInput} is used to store the input for mark an attachment as deleted.
	*/
public record MarkAsDeletedInput(String contentId, UserInfo userInfo) {
}
