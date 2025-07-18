/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ReadonlyDataContextEnhancer;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.utils.OrderConstants;
import com.sap.cds.services.utils.model.CqnUtils;

/**
 * The class {@link UpdateAttachmentsHandler} is an event handler that is called before an update event is executed. As
 * updates in draft entities or non-draft entities can also be create-events, update-events or delete-events the handler
 * needs to distinguish between the different cases.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class UpdateAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(UpdateAttachmentsHandler.class);

	private final ModifyAttachmentEventFactory eventFactory;
	private final AttachmentsReader attachmentsReader;
	private final AttachmentService attachmentService;
	private final ThreadDataStorageReader storageReader;

	public UpdateAttachmentsHandler(ModifyAttachmentEventFactory eventFactory, AttachmentsReader attachmentsReader,
			AttachmentService attachmentService, ThreadDataStorageReader storageReader) {
		this.eventFactory = eventFactory;
		this.attachmentsReader = attachmentsReader;
		this.attachmentService = attachmentService;
		this.storageReader = storageReader;
	}

	@Before(entity = "*")
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
	public void processBeforeForDraft(CdsUpdateEventContext context, List<? extends CdsData> attachments) {
		ReadonlyDataContextEnhancer.enhanceReadonlyDataInContext(context, attachments, storageReader.get());
	}

	@Before(entity = "*")
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsUpdateEventContext context, List<Attachments> attachments) {
		CdsEntity target = context.getTarget();
		boolean noContentInData = ApplicationHandlerHelper.noContentFieldInData(target, attachments);
		boolean associationsAreUnchanged = associationsAreUnchanged(target, attachments);
		if (noContentInData && associationsAreUnchanged) {
			return;
		}

		logger.debug("Processing before update event for entity {}", target.getName());

		CqnSelect select = CqnUtils.toSelect(context.getCqn(), context.getTarget());
		List<Attachments> existingAttachments = attachmentsReader.readAttachments(context.getModel(), target, select);

		List<Attachments> condensedAttachments = ApplicationHandlerHelper.condenseData(existingAttachments, target);
		ModifyApplicationHandlerHelper.handleAttachmentForEntities(target, attachments, condensedAttachments,
				eventFactory, context);

		if (!associationsAreUnchanged) {
			deleteRemovedAttachments(existingAttachments, attachments, target, context.getUserInfo());
		}
	}

	private boolean associationsAreUnchanged(CdsEntity entity, List<Attachments> attachments) {
		// TODO: check if this should be replaced with entity.assocations().noneMatch(...)
		return entity.compositions()
				.noneMatch(association -> attachments.stream().anyMatch(d -> d.containsKey(association.getName())));
	}

	private void deleteRemovedAttachments(List<Attachments> existingAttachments, List<Attachments> updatedAttachments,
			CdsEntity entity, UserInfo userInfo) {
		List<Attachments> condensedUpdatedAttachments = ApplicationHandlerHelper.condenseData(updatedAttachments,
				entity);

		Validator validator = (path, element, value) -> {
			Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
			boolean entryExists = condensedUpdatedAttachments.stream()
					.anyMatch(updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData));
			if (!entryExists) {
				String contentId = (String) path.target().values().get(Attachments.CONTENT_ID);
				attachmentService.markAttachmentAsDeleted(new MarkAsDeletedInput(contentId, userInfo));
			}
		};
		CdsDataProcessor.create().addValidator(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, validator)
				.process(existingAttachments, entity);
	}

}
