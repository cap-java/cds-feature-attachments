package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import java.util.List;

import com.sap.cds.ql.cqn.Modifier;

public class DefaultItemModifierProvider implements ItemModifierProvider {

	@Override
	public Modifier getBeforeReadDocumentIdEnhancer(List<String> mediaAssociations) {
		return new BeforeReadItemsModifier(mediaAssociations);
	}

}
