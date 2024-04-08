package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
	* The class {@link DoNothingAttachmentEvent} does nothing.
	* The event factory uses this class to create an event that does nothing.
	*/
public class DoNothingAttachmentEvent implements ModifyAttachmentEvent {

	private static final Logger logger = LoggerFactory.getLogger(DoNothingAttachmentEvent.class);

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		logger.debug("Do nothing event for entity {}", path.target().entity().getQualifiedName());

		return value;
	}

}
