package com.sap.cds.feature.attachments.integrationtests.common;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Delete;
import com.sap.cds.services.persistence.PersistenceService;

@Component
public class TableDataDeleter {

	@Autowired
	private PersistenceService persistenceService;

	public void deleteData(String... entityNames) {
		Arrays.stream(entityNames).forEach(entityName -> persistenceService.run(Delete.from(entityName)));
	}

}
