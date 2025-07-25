/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetListener;

/**
 * The class {@link CreateAttachmentEvent} handles the creation of an attachment. It calls the {@link AttachmentService}
 * to create the attachment and registers the transaction listener to be able to revert the creation in case of errors.
 */
public class CreateAttachmentEvent implements ModifyAttachmentEvent {

	private static final Logger logger = LoggerFactory.getLogger(CreateAttachmentEvent.class);

	private final AttachmentService attachmentService;
	private final ListenerProvider listenerProvider;

	public CreateAttachmentEvent(AttachmentService attachmentService, ListenerProvider listenerProvider) {
		this.attachmentService = requireNonNull(attachmentService, "attachmentService must not be null");
		this.listenerProvider = requireNonNull(listenerProvider, "listenerProvider must not be null");
	}

	@Override
	public InputStream processEvent(Path path, InputStream content, Attachments attachment, EventContext eventContext) {
		CdsEntity target = path.target().entity();
		logger.debug("Calling attachment service with create event for entity {}", target);

		Map<String, Object> values = path.target().values();
		Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
		String mimeType = getFieldValue(MediaData.MIME_TYPE, values, attachment).orElse(null);
		String fileName = getFieldValue(MediaData.FILE_NAME, values, attachment).orElse(null);

		Map<String, Object> parentIds = getParentIds(target, attachment);

		// call the attachment service to create the attachment
		CreateAttachmentInput createEventInput = new CreateAttachmentInput(keys, target, fileName, mimeType, content,
				parentIds);
		AttachmentModificationResult result = attachmentService.createAttachment(createEventInput);

		// create and register the listener to be able to revert the creation in case of errors
		ChangeSetListener createListener = listenerProvider.provideListener(result.contentId(),
				eventContext.getCdsRuntime());
		eventContext.getChangeSetContext().register(createListener);

		path.target().values().put(Attachments.CONTENT_ID, result.contentId());
		path.target().values().put(Attachments.STATUS, result.status());
		return result.isInternalStored() ? content : null;
	}

	@VisibleForTesting
	static Map<String, Object> getParentIds(CdsEntity target, Attachments attachment) {
		// find "up_" association to parent entity
		Optional<CdsElement> upAssociation = target.findAssociation("up_");

		// if association is found, try to get foreign key to parent entity
		if (upAssociation.isPresent()) {
			// get association type
			CdsAssociationType assocType = upAssociation.get().getType();
			Map<String, Object> parentIds = new HashMap<>();
			// get refs of the association and read the corresponding values from the data of the entity
			assocType.refs().forEach(ref -> {
				String key = "up__" + ref.path();
				Object value = attachment.get(key);
				if (nonNull(value)) {
					parentIds.put(key, value);
				}
			});
			return parentIds;
		}
		// if no association is found, return empty map
		return Collections.emptyMap();
	}

	private static Optional<String> getFieldValue(String fieldName, Map<String, Object> values,
			Attachments attachment) {
		Object annotationValue = values.get(fieldName);
		Object value = nonNull(annotationValue) ? annotationValue : attachment.get(fieldName);
		return Optional.ofNullable((String) value);
	}
}
