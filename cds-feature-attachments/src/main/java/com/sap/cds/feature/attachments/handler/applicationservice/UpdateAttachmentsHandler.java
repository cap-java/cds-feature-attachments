package com.sap.cds.feature.attachments.handler.applicationservice;

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
import com.sap.cds.ql.cqn.CqnFilterableStatement;
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
import com.sap.cds.services.utils.model.CqnUtils;

//TODO add Java Doc
@ServiceName(value = "*", type = ApplicationService.class)
public class UpdateAttachmentsHandler implements EventHandler {

	private final ModifyAttachmentEventFactory eventFactory;
	private final AttachmentsReader attachmentsReader;
	private final AttachmentService outboxedAttachmentService;

	public UpdateAttachmentsHandler(ModifyAttachmentEventFactory eventFactory, AttachmentsReader attachmentsReader, AttachmentService outboxedAttachmentService) {
		this.eventFactory = eventFactory;
		this.attachmentsReader = attachmentsReader;
		this.outboxedAttachmentService = outboxedAttachmentService;
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
		//TODO not needed if media entity direct changed
		var select = getSelect(context.getCqn(), context.getTarget());
		var attachments = attachmentsReader.readAttachments(context.getModel(), target, select);

		var condensedAttachments = ApplicationHandlerHelper.condenseData(attachments, target);
		//TODO check if data.size() == attachments.size() is needed
		if (!isMediaEntity(target) || data.size() == attachments.size()) {
			ModifyApplicationHandlerHelper.handleAttachmentForEntities(target, data, condensedAttachments, eventFactory, context);
		}

		if (!associationsAreUnchanged) {
			deleteRemovedAttachments(attachments, data, target);
		}
	}

	private boolean associationsAreUnchanged(CdsEntity entity, List<CdsData> data) {
		//TODO check only compositions
		return entity.associations()
											.noneMatch(association -> data.stream().anyMatch(d -> d.containsKey(association.getName())));
	}

	private CqnFilterableStatement getSelect(CqnUpdate update, CdsEntity target) {
		return CqnUtils.toSelect(update, target);
	}

	private void deleteRemovedAttachments(List<CdsData> exitingDataList, List<CdsData> updatedDataList, CdsEntity entity) {
		var condensedUpdatedData = ApplicationHandlerHelper.condenseData(updatedDataList, entity);
		var filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Validator validator = (path, element, value) -> {
			var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
			var entryExists = condensedUpdatedData.stream()
																							.anyMatch(updatedData -> ApplicationHandlerHelper.isKeyInData(keys, updatedData));
			if (!entryExists) {
				outboxedAttachmentService.markAsDeleted((String) path.target().values().get(Attachments.DOCUMENT_ID));
			}
		};
		ApplicationHandlerHelper.callValidator(entity, exitingDataList, filter, validator);
	}

	private boolean isMediaEntity(CdsStructuredType entity) {
		return entity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}


}
