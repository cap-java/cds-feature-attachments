package com.sap.cds.feature.attachments.handler.draftservice.modifier;

import com.sap.cds.ql.cqn.Modifier;

public interface ActiveEntityModifierProvider {

	Modifier getModifier(boolean isActiveEntity, String fullEntityName);

}
