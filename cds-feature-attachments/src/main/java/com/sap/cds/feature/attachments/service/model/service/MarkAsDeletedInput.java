/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.service;

import com.sap.cds.services.request.UserInfo;

/**
 * This record is used to store the input for mark an attachment as deleted.
 * 
 * @param contentId The content id of the attachment.
 * @param userInfo  The user information of the user who is marking the attachment as deleted.
 */
public record MarkAsDeletedInput(String contentId, UserInfo userInfo) {
}
