/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

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
	public InputStream processEvent(Path path, InputStream content, CdsData existingData, EventContext eventContext) {
		logger.debug("Calling attachment service with create event for entity {}",
				path.target().entity().getQualifiedName());
		var values = path.target().values();
		var keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
		var mimeTypeOptional = getFieldValue(MediaData.MIME_TYPE, values, existingData);
		var fileNameOptional = getFieldValue(MediaData.FILE_NAME, values, existingData);

		var createEventInput = new CreateAttachmentInput(keys, path.target().entity(), fileNameOptional.orElse(null),
				mimeTypeOptional.orElse(null), content);
		var result = attachmentService.createAttachment(createEventInput);
		var createListener = listenerProvider.provideListener(result.contentId(), eventContext.getCdsRuntime());
		var context = eventContext.getChangeSetContext();
		context.register(createListener);
		path.target().values().put(Attachments.CONTENT_ID, result.contentId());
		path.target().values().put(Attachments.STATUS, result.status());
		return result.isInternalStored() ? content : null;
	}

	private static Optional<String> getFieldValue(String fieldName, Map<String, Object> values, CdsData existingData) {
		var annotationValue = values.get(fieldName);
		var value = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(fieldName);
		return Optional.ofNullable((String) value);
	}
}
