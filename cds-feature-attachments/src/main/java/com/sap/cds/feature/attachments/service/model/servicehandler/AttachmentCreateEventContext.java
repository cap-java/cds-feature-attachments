/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
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
	 * Creates an {@link EventContext} already overlaid with this interface. The event is set to
	 * {@link AttachmentService#EVENT_CREATE_ATTACHMENT}.
	 *
	 * @return the {@link AttachmentCreateEventContext}
	 */
	static AttachmentCreateEventContext create() {
		return EventContext.create(AttachmentCreateEventContext.class, null);
	}

	/**
	 * Returns the ID of the attachment storage entity.
	 *
	 * @return the ID of the attachment storage entity, or {@code null} if no ID was specified
	 */
	String getContentId();

	/**
	 * Sets the ID of the content for the attachment storage.
	 *
	 * @param contentId the ID of the content
	 */
	void setContentId(String contentId);

	/**
	 * Returns the IDs of the attachment storage entity.
	 *
	 * @return the IDs of the attachment storage entity, or {@code java.util.Collections#emptyMap()} if no ID was
	 *         specified
	 */
	Map<String, Object> getAttachmentIds();

	/**
	 * Sets the IDs of the attachment entity for the attachment storage.
	 *
	 * @param ids the IDs of the attachment entity which defines the content field
	 */
	void setAttachmentIds(Map<String, Object> ids);

	/**
	 * Returns the attachment entity for the attachment storage.
	 *
	 * @return the attachment entity which defines the content field
	 */
	CdsEntity getAttachmentEntity();

	/**
	 * Sets the attachment entity for the attachment storage. The name of this entity can be used, for example, to
	 * access the data using the persistence service.
	 *
	 * @param attachmentEntity the attachment entity which defines the content field
	 */
	void setAttachmentEntity(CdsEntity attachmentEntity);

	/**
	 * Returns the data of the content.
	 *
	 * @return the data of the content
	 */
	MediaData getData();

	/**
	 * Sets the data of the attachment to be read.
	 *
	 * @param data the data of the content
	 */
	void setData(MediaData data);

	/**
	 * Indicates whether the content will be internally stored in the database.
	 *
	 * @return {@code true} if the content will be internally stored; {@code false} otherwise
	 */
	Boolean getIsInternalStored();

	/**
	 * Sets the flag indicating whether the content will be internally stored in the database.
	 *
	 * @param isInternalStored {@code true} if the content will be internally stored; {@code false} otherwise
	 */
	void setIsInternalStored(Boolean isInternalStored);

	/**
	 * Returns the IDs of the attachment's parent entity.
	 * <p>
	 * <b>Known limitation:</b> Works only for composition of aspects, because it requires the <code>_up</code> link to
	 * the parent entity. Not supported for composition to entities including the Attachments aspect.
	 * </p>
	 *
	 * @return the IDs of the attachment's parent entity, or {@code java.util.Collections#emptyMap()} if not available
	 */
	Map<String, Object> getParentIds();

	/**
	 * Sets the IDs of the attachment's parent entity.
	 *
	 * @param ids the IDs of the attachment's parent entity
	 */
	void setParentIds(Map<String, Object> ids);

	/**
	 * Returns the parent entity of the attachment.
	 * <p>
	 * <b>Known limitation:</b> Works only for composition of aspects, because it requires the <code>_up</code> link to
	 * the parent entity. Not supported for composition to entities including the Attachments aspect.
	 * </p>
	 *
	 * @return the parent entity of the attachment, or {@code null} if not available
	 */
	CdsEntity getParentEntity();

	/**
	 * Sets the parent entity of the attachment.
	 *
	 * @param parent the parent entity of the attachment
	 */
	void setParentEntity(CdsEntity parent);
}