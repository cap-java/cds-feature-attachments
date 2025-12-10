package customer.bookshop.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.draft.DraftCreateEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

import java.util.List;

/**
 * Custom handler for Attachments to set the uploadStatus field before creation.
 * Handles both draft and non-draft scenarios.
 */
@Component
@ServiceName(value = "*", type = {ApplicationService.class, DraftService.class})
public class AttachmentHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentHandler.class);

	/**
	 * Before creating an attachment (non-draft), set the uploadStatus to 'Pending'.
	 * 
	 * @param context the create event context
	 * @param data the list of data being created
	 */
	@Before
	@HandlerOrder(HandlerOrder.EARLY)
	public void beforeCreateAttachment(CdsCreateEventContext context, List<CdsData> data) {
		// Check if the target entity is an attachment entity
		if (ApplicationHandlerHelper.isMediaEntity(context.getTarget())) {
			logger.info("Setting uploadStatus for {} attachments (CREATE)", data.size());
			for (CdsData item : data) {
				// Set the default uploadStatus to 'Pending' when creating an attachment
				if (!item.containsKey("uploadStatus") || item.get("uploadStatus") == null) {
					item.put("uploadStatus", "Pending");
					logger.info("Set uploadStatus to Pending for attachment");
				}
			}
		}
	}

	/**
	 * Before creating a draft attachment, set the uploadStatus to 'Pending'.
	 * 
	 * @param context the draft create event context
	 * @param data the data being created
	 */
	@Before
	@HandlerOrder(HandlerOrder.EARLY)
	public void beforeDraftCreateAttachment(DraftCreateEventContext context, CdsData data) {
		// Check if the target entity is an attachment entity
		if (ApplicationHandlerHelper.isMediaEntity(context.getTarget())) {
			logger.info("Setting uploadStatus for draft attachment (DRAFT CREATE)");
			// Set the default uploadStatus to 'Pending' when creating an attachment
			if (!data.containsKey("uploadStatus") || data.get("uploadStatus") == null) {
				data.put("uploadStatus", "Pending");
				logger.info("Set uploadStatus to Pending for draft attachment");
			}
		}
	}

	/**
	 * Before patching a draft attachment, set the uploadStatus to 'Pending'.
	 * 
	 * @param context the draft patch event context
	 * @param data the data being patched
	 */
	@Before
	@HandlerOrder(HandlerOrder.EARLY)
	public void beforeDraftPatchAttachment(DraftPatchEventContext context, CdsData data) {
		// Check if the target entity is an attachment entity
		if (ApplicationHandlerHelper.isMediaEntity(context.getTarget())) {
			logger.info("Setting uploadStatus for draft attachment (DRAFT PATCH)");
			if (!data.containsKey("uploadStatus") || data.get("uploadStatus") == null) {
				data.put("uploadStatus", "Pending");
				logger.info("Set uploadStatus to Pending for draft patch attachment");
			}
		}
	}
}
