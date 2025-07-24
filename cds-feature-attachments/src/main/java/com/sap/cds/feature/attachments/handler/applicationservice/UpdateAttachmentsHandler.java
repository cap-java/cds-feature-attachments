/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

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
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
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
		this.eventFactory = requireNonNull(eventFactory, "eventFactory must not be null");
		this.attachmentsReader = requireNonNull(attachmentsReader, "attachmentsReader must not be null");
		this.attachmentService = requireNonNull(attachmentService, "attachmentService must not be null");
		this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
	}

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
	void processBeforeForDraft(CdsUpdateEventContext context, List<CdsData> data) {
		ReadonlyDataContextEnhancer.backupReadonlyFields(context.getTarget(), data, storageReader.get());
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	void processBefore(CdsUpdateEventContext context, List<CdsData> data) {
		CdsEntity target = context.getTarget();
		boolean associationsAreUnchanged = associationsAreUnchanged(target, data);

		if (ApplicationHandlerHelper.noContentFieldInData(target, data) && associationsAreUnchanged) {
			return;
		}

		logger.debug("Processing before update event for entity {}", target.getName());

		CqnSelect select = CqnUtils.toSelect(context.getCqn(), context.getTarget());
		List<Attachments> attachments = attachmentsReader.readAttachments(context.getModel(), target, select);

		List<Attachments> condensedAttachments = ApplicationHandlerHelper.condenseAttachments(attachments, target);
		ModifyApplicationHandlerHelper.handleAttachmentForEntities(target, data, condensedAttachments, eventFactory,
				context);

		if (!associationsAreUnchanged) {
			deleteRemovedAttachments(attachments, data, target, context.getUserInfo());
		}
	}

	private boolean associationsAreUnchanged(CdsEntity entity, List<CdsData> data) {
		// TODO: check if this should be replaced with entity.assocations().noneMatch(...)
		return entity.compositions()
				.noneMatch(association -> data.stream().anyMatch(d -> d.containsKey(association.getName())));
	}

	private void deleteRemovedAttachments(List<Attachments> existingAttachments, List<CdsData> data, CdsEntity entity,
			UserInfo userInfo) {
		List<Attachments> condensedAttachments = ApplicationHandlerHelper.condenseAttachments(data, entity);

		Validator validator = (path, element, value) -> {
			Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
			boolean entryExists = condensedAttachments.stream()
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
