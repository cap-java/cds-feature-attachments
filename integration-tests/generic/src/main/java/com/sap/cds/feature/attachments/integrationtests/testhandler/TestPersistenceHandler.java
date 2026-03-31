/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static com.sap.cds.services.cds.CqnService.EVENT_CREATE;
import static com.sap.cds.services.cds.CqnService.EVENT_UPDATE;

import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import org.springframework.stereotype.Component;

@ServiceName(value = "*", type = PersistenceService.class)
@Component
public class TestPersistenceHandler implements EventHandler {

  private volatile boolean throwExceptionOnUpdate = false;
  private volatile boolean throwExceptionOnCreate = false;

  @Before(event = EVENT_UPDATE)
  public void throwExceptionOnUpdate() {
    if (throwExceptionOnUpdate) {
      throw new ServiceException("Exception on update");
    }
  }

  @Before(event = EVENT_CREATE)
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
