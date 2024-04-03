package com.sap.cds.feature.attachments.handler.draftservice.modifier;

import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.reflect.CdsEntity;

public interface ActiveEntityModifierProvider {

	Modifier getModifier(CdsEntity root, boolean isActiveEntity);

}
