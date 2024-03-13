package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.ArrayList;
import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class UpdateAttachmentsHandler implements EventHandler {

	private final ModifyAttachmentEventFactory eventFactory;
	private final AttachmentsReader attachmentsReader;
	private final AttachmentService attachmentService;

	public UpdateAttachmentsHandler(ModifyAttachmentEventFactory eventFactory, AttachmentsReader attachmentsReader, AttachmentService attachmentService) {
		this.eventFactory = eventFactory;
		this.attachmentsReader = attachmentsReader;
		this.attachmentService = attachmentService;
	}

	@Before(event = CqnService.EVENT_UPDATE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsUpdateEventContext context, List<CdsData> data) {
		var target = context.getTarget();
		var noContentInData = !ApplicationHandlerHelper.isContentFieldInData(target, data);
		var associationsAreUnchanged = associationsAreUnchanged(target, data);
		if (noContentInData && associationsAreUnchanged) {
			return;
		}
		var select = getSelect(target, context.getCqn(), data);
		var attachments = attachmentsReader.readAttachments(context.getModel(), target, select);

		var condensedAttachments = ApplicationHandlerHelper.condenseData(attachments, target);
		if (!isMediaEntity(target) || isMediaEntity(target) && data.size() == attachments.size()) {
			ModifyApplicationHandlerHelper.uploadAttachmentForEntity(target, data, condensedAttachments, eventFactory);
		}

		if (!associationsAreUnchanged) {
			deleteRemovedAttachments(attachments, data, target);
		}
	}

	private boolean associationsAreUnchanged(CdsEntity entity, List<CdsData> data) {
		return entity.associations().noneMatch(association -> data.stream().anyMatch(d -> d.containsKey(association.getName())));
	}

	private CqnFilterableStatement getSelect(CdsEntity entity, CqnUpdate update, List<CdsData> data) {
		var filter = update.ref().asRef().rootSegment().filter();
		var where = update.where();
		CqnPredicate resultPredicate;
		if (filter.isPresent() && where.isPresent()) {
			resultPredicate = CQL.and(filter.get(), where.get());
		} else if (filter.isPresent()) {
			resultPredicate = filter.get();
		} else if (where.isPresent()) {
			resultPredicate = where.get();
		} else {
			List<CqnPredicate> predicates = new ArrayList<>();
			data.forEach(d -> {
				var keyData = CdsData.create();
				entity.keyElements().forEach(key -> {
					if (!ApplicationHandlerHelper.DRAFT_ENTITY_ACTIVE_FIELD.equals(key.getName()) && d.containsKey(key.getName())) {
						keyData.put(key.getName(), d.get(key.getName()));
					}
				});
				var select = Select.from(entity.getQualifiedName()).matching(keyData);
				select.where().ifPresent(predicates::add);
			});
			resultPredicate = CQL.or(predicates);
		}
		return Select.from(entity.getQualifiedName()).where(resultPredicate);
	}

	private void deleteRemovedAttachments(List<CdsData> exitingDataList, List<CdsData> updatedDataList, CdsEntity entity) {
		var condensedUpdatedData = ApplicationHandlerHelper.condenseData(updatedDataList, entity);
		var filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Validator validator = (path, element, value) -> {
			var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
			var entryExists = condensedUpdatedData.stream().anyMatch(updatedData -> ApplicationHandlerHelper.isKeyInData(keys, updatedData));
			if (!entryExists) {
				attachmentService.deleteAttachment((String) path.target().values().get(Attachments.DOCUMENT_ID));
			}
		};
		ApplicationHandlerHelper.callValidator(entity, exitingDataList, filter, validator);
	}

	private boolean isMediaEntity(CdsStructuredType entity) {
		return entity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}


}
