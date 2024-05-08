/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.modifier;

import java.util.List;

import com.sap.cds.ql.cqn.Modifier;

/**
	* The interface {@link ItemModifierProvider} provides a method to get the before read content id enhancer.
	*/
public interface ItemModifierProvider {

	Modifier getBeforeReadContentIdEnhancer(List<String> mediaAssociations);

}
