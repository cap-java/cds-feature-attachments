/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.service.model.servicehandler;

import java.util.Map;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

/**
 * The {@link AttachmentCreateEventContext} is used to store the context of the create attachment event.
 */
@EventName(AttachmentService.EVENT_CREATE_ATTACHMENT)
public interface AttachmentCreateEventContext extends EventContext {

	/**
	 * Creates an {@link EventContext} already overlay with this interface. The event is set to be
	 * {@link AttachmentService#EVENT_CREATE_ATTACHMENT}
	 *
	 * @return the {@link AttachmentCreateEventContext}
	 */
	static AttachmentCreateEventContext create() {
		return EventContext.create(AttachmentCreateEventContext.class, null);
	}

	/**
	 * @return The id of the attachment storage entity or {@code null} if no id was specified
	 */
	String getContentId();

	/**
	 * Sets the ID of the content for the attachment storage
	 *
	 * @param contentId The key of the content
	 */
	void setContentId(String contentId);

	/**
	 * @return The IDs of the attachment storage entity or {@code Collections.emptyMap} if no id was specified
	 */
	Map<String, Object> getAttachmentIds();

	/**
	 * Sets the id af the attachment entity for the attachment storage
	 *
	 * @param ids The key of the attachment entity which defines the content field
	 */
	void setAttachmentIds(Map<String, Object> ids);

	/**
	 * The attachment entity for the attachment storage
	 *
	 * @return The attachment entity which defines the content field
	 */
	CdsEntity getAttachmentEntity();

	/**
	 * Sets the attachment entity for the attachment storage The name of this entity can be used e.g. to access the data
	 * using the persistence service
	 *
	 * @param attachmentEntity The attachment entity which defines the content field
	 */
	void setAttachmentEntity(CdsEntity attachmentEntity);

	/**
	 * @return The data of the content
	 */
	MediaData getData();

	/**
	 * Sets the data of the attachment to be read
	 *
	 * @param data The data of the content
	 */
	void setData(MediaData data);

	/**
	 * Flag that shows if the content will be internal stored in the database
	 *
	 * @return The flag for internal storage
	 */
	Boolean getIsInternalStored();

	/**
	 * Sets the flag which show that the content will be internal stored in the database
	 *
	 * @param isInternalStored Flag that the content will be internal stored in the Database
	 */
	void setIsInternalStored(Boolean isInternalStored);

}
