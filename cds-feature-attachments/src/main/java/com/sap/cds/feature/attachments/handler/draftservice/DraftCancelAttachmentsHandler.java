/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.constants.DraftConstants;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifierProvider;
import com.sap.cds.feature.attachments.utilities.LoggingMarker;
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
 * The class {@link DraftCancelAttachmentsHandler} is an event handler that is called
 * before a draft cancel event is executed.
 * The handler checks if the attachments of the draft entity are still valid and
 * deletes the content of the attachments if necessary.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DraftCancelAttachmentsHandler.class);
	private static final Marker marker = LoggingMarker.DRAFT_CANCEL_HANDLER.getMarker();

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;
	private final ActiveEntityModifierProvider activeEntityModifierProvider;

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader,
			ModifyAttachmentEvent deleteContentAttachmentEvent, ActiveEntityModifierProvider activeEntityModifierProvider) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
		this.activeEntityModifierProvider = activeEntityModifierProvider;
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context) {
		if (isWhereEmpty(context)) {
			logger.debug(marker, "Processing before draft cancel event for entity {}", context.getTarget().getName());

			var activeEntity = getActiveEntity(context);
			var draftEntity = getDraftEntity(context);

			var draftAttachments = readAttachments(context, draftEntity, false);
			var activeCondensedAttachments = getCondensedActiveAttachments(context, activeEntity);

			var filter = buildContentIdFilter();
			var validator = buildDeleteContentValidator(context, activeCondensedAttachments);
			ApplicationHandlerHelper.callValidator(context.getTarget(), draftAttachments, filter, validator);
		}

	}

	private Validator buildDeleteContentValidator(DraftCancelEventContext context,
			List<CdsData> activeCondensedAttachments) {
		return (path, element, value) -> {
			if (Boolean.FALSE.equals(path.target().values().get(Drafts.HAS_ACTIVE_ENTITY))) {
				deleteContentAttachmentEvent.processEvent(path, null, CdsData.create(path.target().values()), context);
				return;
			}
			var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
			var existingEntry = activeCondensedAttachments.stream().filter(
					updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData)).findAny();
			existingEntry.ifPresent(entry -> {
				if (!entry.get(Attachments.CONTENT_ID).equals(value)) {
					deleteContentAttachmentEvent.processEvent(null, null, CdsData.create(path.target().values()), context);
				}
			});
		};
	}

	private boolean isWhereEmpty(DraftCancelEventContext context) {
		return context.getCqn().where().isEmpty();
	}

	private CdsStructuredType getActiveEntity(DraftCancelEventContext context) {
		return isDraftEntity(context) ? context.getTarget().getTargetOf(DraftConstants.SIBLING_ENTITY) : context.getTarget();
	}

	private CdsStructuredType getDraftEntity(DraftCancelEventContext context) {
		return isDraftEntity(context) ? context.getTarget() : context.getTarget().getTargetOf(DraftConstants.SIBLING_ENTITY);
	}

	private boolean isDraftEntity(DraftCancelEventContext context) {
		return context.getTarget().getQualifiedName().endsWith(DraftConstants.DRAFT_TABLE_POSTFIX);
	}

	private List<CdsData> readAttachments(DraftCancelEventContext context, CdsStructuredType entity,
			boolean isActiveEntity) {
		var cqnInactiveEntity = CQL.copy(context.getCqn(),
				activeEntityModifierProvider.getModifier(isActiveEntity, entity.getQualifiedName()));
		return attachmentsReader.readAttachments(context.getModel(), (CdsEntity) entity, cqnInactiveEntity);
	}

	private List<CdsData> getCondensedActiveAttachments(DraftCancelEventContext context, CdsStructuredType activeEntity) {
		var attachments = readAttachments(context, activeEntity, true);
		return ApplicationHandlerHelper.condenseData(attachments, context.getTarget());
	}

	private Filter buildContentIdFilter() {
		return (path, element, type) -> ApplicationHandlerHelper.isMediaEntity(path.target().type()) && element.getName()
				.equals(Attachments.CONTENT_ID);
	}

}
