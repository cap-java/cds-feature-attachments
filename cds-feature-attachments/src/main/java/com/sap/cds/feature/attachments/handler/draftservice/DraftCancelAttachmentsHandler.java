package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.constants.DraftConstants;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifierProvider;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;
	private final ActiveEntityModifierProvider activeEntityModifierProvider;

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader, ModifyAttachmentEvent deleteContentAttachmentEvent, ActiveEntityModifierProvider activeEntityModifierProvider) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
		this.activeEntityModifierProvider = activeEntityModifierProvider;
	}

	//TODO Unit Tests
	//TODO refactor
	@Before(event = DraftService.EVENT_DRAFT_CANCEL)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context) {
		var where = context.getCqn().where();
		if (where.isEmpty()) {

			var activeEntity = isDraftEntity(context) ? context.getTarget().getTargetOf("SiblingEntity") : context.getTarget();
			var inactiveEntity = isDraftEntity(context) ? context.getTarget() : context.getTarget().getTargetOf("SiblingEntity");

			var cqnInactiveEntity = CQL.copy(context.getCqn(), activeEntityModifierProvider.getModifier(false, inactiveEntity.getQualifiedName()));
			var draftAttachments = attachmentsReader.readAttachments(context.getModel(), (CdsEntity) inactiveEntity, cqnInactiveEntity);

			var analyser = CqnAnalyzer.create(context.getModel());
			var result = analyser.analyze(context.getCqn());

			var cqnActiveEntity = CQL.copy(context.getCqn(), activeEntityModifierProvider.getModifier(true, activeEntity.getQualifiedName()));
			var activeAttachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget(), cqnActiveEntity);
			var condensedActiveData = ApplicationHandlerHelper.condenseData(activeAttachments, context.getTarget());

			Filter filter = (path, element, type) -> ApplicationHandlerHelper.isMediaEntity(path.target()
																																																																																					.type()) && element.getName()
																																																																																																			.equals(Attachments.DOCUMENT_ID);
			Validator validator = (path, element, value) -> {
				if (Boolean.FALSE.equals(path.target().values().get(Drafts.HAS_ACTIVE_ENTITY))) {
					deleteContentAttachmentEvent.processEvent(path, null, CdsData.create(path.target().values()), context);
					return;
				}
				var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
				var existingEntry = condensedActiveData.stream()
																										.filter(updatedData -> ApplicationHandlerHelper.isKeyInData(keys, updatedData)).findAny();
				existingEntry.ifPresent(entry -> {
					if (!entry.get(Attachments.DOCUMENT_ID).equals(value)) {
						deleteContentAttachmentEvent.processEvent(null, null, CdsData.create(path.target().values()), context);
					}
				});
			};
			ApplicationHandlerHelper.callValidator(result.targetEntity(), draftAttachments, filter, validator);

		}

	}

	private boolean isDraftEntity(DraftCancelEventContext context) {
		return context.getTarget().getQualifiedName().endsWith(DraftConstants.DRAFT_TABLE_POSTFIX);
	}

}
