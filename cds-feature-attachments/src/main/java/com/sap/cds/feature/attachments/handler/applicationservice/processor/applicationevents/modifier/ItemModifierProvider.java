package com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.modifier;

import java.util.List;

import com.sap.cds.ql.cqn.Modifier;

/**
	* The interface {@link ItemModifierProvider} provides a method to get the before read document id enhancer.
	*/
public interface ItemModifierProvider {

	Modifier getBeforeReadDocumentIdEnhancer(List<String> mediaAssociations);

}
