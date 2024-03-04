package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import java.util.Map;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.ql.cqn.Modifier;

public class DefaultItemModifierProvider implements ItemModifierProvider {

	@Override
	public Modifier getBeforeReadDocumentIdEnhancer(Map<String, DocumentFieldNames> fieldNamesMap) {
		return new BeforeReadItemsModifier(fieldNamesMap);
	}

}
