package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnPredicate;
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

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader, ModifyAttachmentEvent deleteContentAttachmentEvent) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
	}

	//TODO Unit Tests
	//TODO refactor
	@Before(event = DraftService.EVENT_DRAFT_CANCEL)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context, List<CdsData> data) {
		var where = context.getCqn().where();
		if (where.isEmpty()) {
			var draftAttachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget()
																																																																																		.getTargetOf("SiblingEntity"), context.getCqn());

			var analyser = CqnAnalyzer.create(context.getModel());
			var result = analyser.analyze(context.getCqn());
			var keyWhere = getWhereBasedOfKeyFields(context.getTarget(), result.targetKeys());

			var activeAttachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget(), keyWhere);

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

	private CqnPredicate getWhereBasedOfKeyFields(CdsEntity entity, Map<String, Object> data) {
		CqnPredicate resultPredicate;
		List<CqnPredicate> predicates = new ArrayList<>();

		var keyData = CdsData.create();
		entity.keyElements().forEach(key -> {
			if (!Drafts.IS_ACTIVE_ENTITY.equals(key.getName()) && data.containsKey(key.getName())) {
				keyData.put(key.getName(), data.get(key.getName()));
			}

			var select = Select.from(entity.getQualifiedName()).matching(keyData);
			select.where().ifPresent(predicates::add);
		});
		resultPredicate = CQL.and(predicates);
		return resultPredicate;
	}
}
