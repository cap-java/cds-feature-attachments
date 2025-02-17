/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.servicehandler;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

/**
 * The {@link AttachmentReadEventContext} is used to store the context of the read attachment event.
 */
@EventName(AttachmentService.EVENT_READ_ATTACHMENT)
public interface AttachmentReadEventContext extends EventContext {

	/**
	 * Creates an {@link EventContext} already overlay with this interface. The event is set to be
	 * {@link AttachmentService#EVENT_READ_ATTACHMENT}
	 *
	 * @return the {@link AttachmentReadEventContext}
	 */
	static AttachmentReadEventContext create() {
		return EventContext.create(AttachmentReadEventContext.class, null);
	}

	/**
	 * @return The data of the content. The data contain the following fields:
	 *         <ul>
	 *         <li>content</li>
	 *         <li>mimeType</li>
	 *         <li>fileName</li>
	 *         </ul>
	 */
	MediaData getData();

	/**
	 * Sets the data of the attachment to be read
	 *
	 * @param data The data of the content
	 */
	void setData(MediaData data);

	/**
	 * @return The ID of the content or {@code null} if no id was specified
	 */
	String getContentId();

	/**
	 * Sets the content id for the attachment to be read
	 *
	 * @param contentId The ID of the content
	 */
	void setContentId(String contentId);

}
