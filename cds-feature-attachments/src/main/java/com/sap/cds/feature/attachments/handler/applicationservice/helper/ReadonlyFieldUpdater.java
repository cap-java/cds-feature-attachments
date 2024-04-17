package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.Map;

import com.sap.cds.ql.Update;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.persistence.PersistenceService;

public class ReadonlyFieldUpdater implements ChangeSetListener {

	private final CdsEntity entity;
	private final Map<String, Object> keys;
	private final Map<String, Object> readonlyFields;
	private final PersistenceService persistence;

	public ReadonlyFieldUpdater(CdsEntity entity, Map<String, Object> keys, Map<String, Object> readonlyFields,
			PersistenceService persistence) {
		this.entity = entity;
		this.keys = keys;
		this.readonlyFields = readonlyFields;
		this.persistence = persistence;
	}

	@Override
	public void beforeClose() {
		var update = Update.entity(entity).data(readonlyFields).matching(keys);
		persistence.run(update);
	}
}
