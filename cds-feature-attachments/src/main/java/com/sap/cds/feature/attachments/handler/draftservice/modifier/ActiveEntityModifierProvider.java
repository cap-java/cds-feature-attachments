package com.sap.cds.feature.attachments.handler.draftservice.modifier;

import com.sap.cds.ql.cqn.Modifier;

/**
	* The interface {@link ActiveEntityModifierProvider} is used to get the modifier
	* for changing the fields {@code isActiveEntity} and {@code fullEntityName}.
	*/
public interface ActiveEntityModifierProvider {

	Modifier getModifier(boolean isActiveEntity, String fullEntityName);

}
