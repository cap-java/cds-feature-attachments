/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.CQL;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link DraftCancelAttachmentsHandler} is an event handler that is
 * called before a draft cancel event is executed. The handler checks if the
 * attachments of the draft entity are still valid and deletes the content of
 * the attachments if necessary.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DraftCancelAttachmentsHandler.class);

	private static final Filter contentIdFilter = (path, element,
			type) -> ApplicationHandlerHelper.isMediaEntity(path.target().type())
					&& element.getName().equals(Attachments.CONTENT_ID);

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader,
			ModifyAttachmentEvent deleteContentAttachmentEvent) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context) {
		if (isWhereEmpty(context)) {
			logger.debug("Processing before draft cancel event for entity {}", context.getTarget().getName());

			var activeEntity = DraftUtils.getActiveEntity(context.getTarget());
			var draftEntity = DraftUtils.getDraftEntity(context.getTarget());

			var draftAttachments = readAttachments(context, draftEntity, false);
			var activeCondensedAttachments = getCondensedActiveAttachments(context, activeEntity);

			var validator = buildDeleteContentValidator(context, activeCondensedAttachments);
			CdsDataProcessor.create().addValidator(contentIdFilter, validator).process(draftAttachments, context.getTarget());
		}
	}

	private Validator buildDeleteContentValidator(DraftCancelEventContext context,
			List<CdsData> activeCondensedAttachments) {
		return (path, element, value) -> {
			if (Boolean.FALSE.equals(path.target().values().get(Drafts.HAS_ACTIVE_ENTITY))) {
				deleteContentAttachmentEvent.processEvent(path, null, CdsData.create(path.target().values()), context);
				return;
			}
			var keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
			var existingEntry = activeCondensedAttachments.stream()
					.filter(updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData)).findAny();
			existingEntry.ifPresent(entry -> {
				if (!entry.get(Attachments.CONTENT_ID).equals(value)) {
					deleteContentAttachmentEvent.processEvent(null, null, CdsData.create(path.target().values()),
							context);
				}
			});
		};
	}

	private boolean isWhereEmpty(DraftCancelEventContext context) {
		return context.getCqn().where().isEmpty();
	}

	private List<CdsData> readAttachments(DraftCancelEventContext context, CdsStructuredType entity,
			boolean isActiveEntity) {
		var cqnInactiveEntity = CQL.copy(context.getCqn(),
				new ActiveEntityModifier(isActiveEntity, entity.getQualifiedName()));
		return attachmentsReader.readAttachments(context.getModel(), (CdsEntity) entity, cqnInactiveEntity);
	}

	private List<CdsData> getCondensedActiveAttachments(DraftCancelEventContext context,
			CdsStructuredType activeEntity) {
		var attachments = readAttachments(context, activeEntity, true);
		return ApplicationHandlerHelper.condenseData(attachments, context.getTarget());
	}
}
