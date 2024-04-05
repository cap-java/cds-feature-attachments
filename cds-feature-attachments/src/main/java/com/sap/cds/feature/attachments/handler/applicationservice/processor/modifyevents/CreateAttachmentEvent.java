package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
	* The class {@link CreateAttachmentEvent} handles the creation of an attachment.
	* It calls the {@link AttachmentService} to create the attachment and registers the
	* transaction listener to be able to revert the creation in case of errors.
	*/
public class CreateAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;
	private final AttachmentService outboxedAttachmentService;
	private final ListenerProvider listenerProvider;

	public CreateAttachmentEvent(AttachmentService attachmentService, AttachmentService outboxedAttachmentService, ListenerProvider listenerProvider) {
		this.attachmentService = attachmentService;
		this.outboxedAttachmentService = outboxedAttachmentService;
		this.listenerProvider = listenerProvider;
	}

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		var values = path.target().values();
		var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
		var mimeTypeOptional = getFieldValue(MediaData.MIME_TYPE, values, existingData);
		var fileNameOptional = getFieldValue(MediaData.FILE_NAME, values, existingData);

		var createEventInput = new CreateAttachmentInput(keys, path.target().entity()
																																																																				.getQualifiedName(), fileNameOptional.orElse(null), mimeTypeOptional.orElse(null), (InputStream) value);
		var result = attachmentService.createAttachment(createEventInput);
		var listener = listenerProvider.provideListener(result.documentId(), eventContext.getCdsRuntime(), outboxedAttachmentService);
		var context = eventContext.getChangeSetContext();
		context.register(listener);
		path.target().values().put(Attachments.DOCUMENT_ID, result.documentId());
		path.target().values().put(Attachments.STATUS_CODE, result.attachmentStatus());
		return result.isInternalStored() ? value : null;
	}

	private static Optional<String> getFieldValue(String fieldName, Map<String, Object> values, CdsData existingData) {
		var annotationValue = values.get(fieldName);
		var value = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(fieldName);
		return Optional.ofNullable((String) value);
	}

}
