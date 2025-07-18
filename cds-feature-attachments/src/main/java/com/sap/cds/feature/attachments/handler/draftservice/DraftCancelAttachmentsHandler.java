/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.MarkAsDeletedAttachmentEvent;
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

/**
 * The class {@link DraftCancelAttachmentsHandler} is an event handler that is called before a draft cancel event is
 * executed. The handler checks if the attachments of the draft entity are still valid and deletes the content of the
 * attachments if necessary.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DraftCancelAttachmentsHandler.class);

	private static final Filter contentIdFilter = (path, element,
			type) -> ApplicationHandlerHelper.isMediaEntity(path.target().type())
					&& element.getName().equals(Attachments.CONTENT_ID);

	private final AttachmentsReader attachmentsReader;
	private final MarkAsDeletedAttachmentEvent deleteEvent;

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader,
			MarkAsDeletedAttachmentEvent deleteEvent) {
		this.attachmentsReader = requireNonNull(attachmentsReader, "attachmentsReader must not be null");
		this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context) {
		if (isWhereEmpty(context)) {
			logger.debug("Processing before draft cancel event for entity {}", context.getTarget().getName());

			CdsEntity activeEntity = DraftUtils.getActiveEntity(context.getTarget());
			CdsEntity draftEntity = DraftUtils.getDraftEntity(context.getTarget());

			List<Attachments> draftAttachments = readAttachments(context, draftEntity, false);
			List<Attachments> activeCondensedAttachments = getCondensedActiveAttachments(context, activeEntity);

			Validator validator = buildDeleteContentValidator(context, activeCondensedAttachments);
			CdsDataProcessor.create().addValidator(contentIdFilter, validator).process(draftAttachments,
					context.getTarget());
		}
	}

	private Validator buildDeleteContentValidator(DraftCancelEventContext context,
			List<? extends CdsData> activeCondensedAttachments) {
		return (path, element, value) -> {
			Attachments attachment = Attachments.of(path.target().values());
			if (Boolean.FALSE.equals(attachment.get(Drafts.HAS_ACTIVE_ENTITY))) {
				deleteEvent.processEvent(path, null, attachment, context);
				return;
			}
			var keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
			var existingEntry = activeCondensedAttachments.stream()
					.filter(updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData)).findAny();
			existingEntry.ifPresent(entry -> {
				if (!entry.get(Attachments.CONTENT_ID).equals(value)) {
					deleteEvent.processEvent(null, null, attachment, context);
				}
			});
		};
	}

	private boolean isWhereEmpty(DraftCancelEventContext context) {
		return context.getCqn().where().isEmpty();
	}

	private List<Attachments> readAttachments(DraftCancelEventContext context, CdsStructuredType entity,
			boolean isActiveEntity) {
		var cqnInactiveEntity = CQL.copy(context.getCqn(),
				new ActiveEntityModifier(isActiveEntity, entity.getQualifiedName()));
		return attachmentsReader.readAttachments(context.getModel(), (CdsEntity) entity, cqnInactiveEntity);
	}

	private List<Attachments> getCondensedActiveAttachments(DraftCancelEventContext context,
			CdsStructuredType activeEntity) {
		var attachments = readAttachments(context, activeEntity, true);
		return ApplicationHandlerHelper.condenseData(attachments, context.getTarget());
	}
}
