package com.sap.cds.feature.attachments.integrationtests.testhandler;

import org.springframework.stereotype.Component;

import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

@ServiceName(value = "*", type = PersistenceService.class)
@Component
public class TestPersistenceHandler implements EventHandler {

	private boolean throwExceptionOnUpdate = false;
	private boolean throwExceptionOnCreate = false;

	@Before(event = PersistenceService.EVENT_UPDATE)
	public void throwExceptionOnUpdate() {
		if (throwExceptionOnUpdate) {
			throw new ServiceException("Exception on update");
		}
	}

	@Before(event = PersistenceService.EVENT_CREATE)
	public void throwExceptionOnCreate() {
		if (throwExceptionOnCreate) {
			throw new ServiceException("Exception on create");
		}
	}

	public void reset() {
		throwExceptionOnUpdate = false;
		throwExceptionOnCreate = false;
	}

	public void setThrowExceptionOnUpdate(boolean throwExceptionOnUpdate) {
		this.throwExceptionOnUpdate = throwExceptionOnUpdate;
	}

	public void setThrowExceptionOnCreate(boolean throwExceptionOnCreate) {
		this.throwExceptionOnCreate = throwExceptionOnCreate;
	}

}
