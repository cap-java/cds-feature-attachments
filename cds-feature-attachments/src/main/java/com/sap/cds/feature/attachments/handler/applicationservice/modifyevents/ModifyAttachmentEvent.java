/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import java.io.InputStream;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
 * The interface {@link ModifyAttachmentEvent} provides a method to process an event on the {@link AttachmentService}.
 */
public interface ModifyAttachmentEvent {

	/**
	 * Processes the event on the {@link AttachmentService}.
	 * 
	 * @param path         the path of the attachment
	 * @param content      the content of the attachment
	 * @param attachment   existing attachment data
	 * @param eventContext the current event context
	 * @return the processed content
	 */
	InputStream processEvent(Path path, InputStream content, Attachments attachment, EventContext eventContext);

}
