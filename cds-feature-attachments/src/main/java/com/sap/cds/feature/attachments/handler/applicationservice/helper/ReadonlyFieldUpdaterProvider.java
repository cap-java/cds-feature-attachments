package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.Map;

import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.changeset.ChangeSetListener;

public interface ReadonlyFieldUpdaterProvider {

	ChangeSetListener getReadonlyFieldUpdater(CdsEntity entity, Map<String, Object> keys,
																																											Map<String, Object> readonlyFields);

}
