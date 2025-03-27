/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.fs.handler;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftCreateEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * Event handler for draft creation events on the DraftService.
 */
@ServiceName(value = "*", type = DraftService.class)
public class FSDraftCreateHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(FSDraftCreateHandler.class);

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	void onCreateDraftAttachment(DraftCreateEventContext context, CdsData data) {
		CdsEntity target = context.getTarget();

		// check if target entity contains aspect Attachments
		if (ApplicationHandlerHelper.isMediaEntity(target)) {
			String fileName = (String) data.get(Attachments.FILE_NAME);

			// get unique identifier of attachment's parent entity, e.g. the Books entity
			Parent parent = getParentId(target, data);

			logger.info("Creating draft attachment '{}' for parent entity '{}' with ids {}", fileName,
					parent != null ? parent.entity : "unknown", parent != null ? parent.ids : "unknown");

			// do something with the data of the draft attachments entity
		}
	}

	private static Parent getParentId(CdsEntity target, CdsData data) {
		// find association to parent entity
		Optional<CdsElement> upAssociation = target.findAssociation("up_");

		// if association is found, try to get foreign key to parent entity
		if (upAssociation.isPresent()) {
			// get association type
			CdsAssociationType assocType = upAssociation.get().getType();
			// get the refs of the association and map them to the corresponding data of the entity
			List<Object> ids = assocType.refs().map(ref -> "up__" + ref.path()).map(data::get).toList();
			return new Parent(assocType.getTarget(), ids);
		}
		return null;
	}

	record Parent(CdsEntity entity, List<Object> ids) {
	}
}