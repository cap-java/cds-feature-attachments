package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import java.util.Map;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.ql.cqn.Modifier;

public interface ItemModifierProvider {

		Modifier getBeforeReadDocumentIdEnhancer(Map<String, DocumentFieldNames> fieldNamesMap);

}
