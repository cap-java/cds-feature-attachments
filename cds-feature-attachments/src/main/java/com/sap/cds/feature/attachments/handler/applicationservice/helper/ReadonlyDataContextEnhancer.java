/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.List;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.services.EventContext;

public final class ReadonlyDataContextEnhancer {

	private static final String CREATE_READONLY_CONTEXT = "CREATE_READONLY_CONTEXT";

	public static void enhanceReadonlyDataInContext(EventContext context, List<CdsData> data, boolean isDraft) {
		var filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Validator validator = (path, element, value) -> {
			if (isDraft) {
				var contentId = path.target().values().get(Attachments.CONTENT_ID);
				var statusCode = path.target().values().get(Attachments.STATUS);
				var scannedAt = path.target().values().get(Attachments.SCANNED_AT);
				var cdsData = CdsData.create();
				cdsData.put(Attachments.CONTENT_ID, contentId);
				cdsData.put(Attachments.STATUS, statusCode);
				cdsData.put(Attachments.SCANNED_AT, scannedAt);
				path.target().values().put(CREATE_READONLY_CONTEXT, cdsData);
			} else {
				path.target().values().remove(CREATE_READONLY_CONTEXT);
			}
		};

		ApplicationHandlerHelper.callValidator(context.getTarget(), data, filter, validator);
	}

	public static void fillReadonlyInContext(CdsData data) {
		var readOnlyData = (CdsData) data.get(CREATE_READONLY_CONTEXT);
		if (Objects.nonNull(readOnlyData)) {
			data.put(Attachments.CONTENT_ID, readOnlyData.get(Attachments.CONTENT_ID));
			data.put(Attachments.STATUS, readOnlyData.get(Attachments.STATUS));
			data.put(Attachments.SCANNED_AT, readOnlyData.get(Attachments.SCANNED_AT));
			data.remove(CREATE_READONLY_CONTEXT);
		}
	}

	private ReadonlyDataContextEnhancer() {
	}


}
