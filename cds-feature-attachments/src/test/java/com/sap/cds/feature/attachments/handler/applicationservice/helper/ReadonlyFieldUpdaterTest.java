package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.persistence.PersistenceService;

class ReadonlyFieldUpdaterTest {

	private ReadonlyFieldUpdater cut;
	private CdsEntity entity;
	private Map<String, Object> keys;
	private Map<String, Object> readonlyFields;
	private PersistenceService persistence;
	private ArgumentCaptor<CqnUpdate> captor;

	@BeforeEach
	void setup() {
		entity = mock(CdsEntity.class);
		keys = Map.of("key", "value");
		readonlyFields = Map.of("readonly", "value for readonly field");
		persistence = mock(PersistenceService.class);
		cut = new ReadonlyFieldUpdater(entity, keys, readonlyFields, persistence);

		captor = ArgumentCaptor.forClass(CqnUpdate.class);
	}

	@Test
	void persistenceServiceCalled() {
		cut.beforeClose();

		verify(persistence).run(captor.capture());
		var update = captor.getValue();
		assertThat(update.entries()).contains(readonlyFields);
		assertThat(update.where()).isPresent();
		assertThat(update.where().get().toString()).contains("[{\"ref\":[\"key\"]},\"=\",{\"val\":\"value\"}]");
	}

}