package com.sap.cds.feature.attachments.handler.draftservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.Drafts;

class DraftUtilsTest {

	private static CdsEntity draftEntity;

	private static CdsEntity activeEntity;

	@BeforeAll
	static void setUpBeforeClass() {
		draftEntity = mock(CdsEntity.class);
		activeEntity = mock(CdsEntity.class);

		when(draftEntity.getQualifiedName()).thenReturn("TestEntity_drafts");
		when(draftEntity.getTargetOf(Drafts.SIBLING_ENTITY)).thenReturn(activeEntity);

		when(activeEntity.getQualifiedName()).thenReturn("TestEntity");
		when(activeEntity.getTargetOf(Drafts.SIBLING_ENTITY)).thenReturn(draftEntity);
	}

	@Test
	void testGetActiveEntity() {
		CdsEntity entity = DraftUtils.getActiveEntity(draftEntity);
		assertNotNull(entity, "Active entity should not be null");
		assertEquals(activeEntity, entity, "Active entity should match the expected active entity");

		entity = DraftUtils.getActiveEntity(activeEntity);
		assertEquals(activeEntity, entity, "Active entity should match the expected active entity");
	}

	@Test
	void testGetDraftEntity() {
		CdsEntity entity = DraftUtils.getDraftEntity(activeEntity);
		assertNotNull(entity, "Draft entity should not be null");
		assertEquals(draftEntity, entity, "Draft entity should match the expected draft entity");

		entity = DraftUtils.getDraftEntity(draftEntity);
		assertEquals(draftEntity, entity, "Draft entity should match the expected draft entity");
	}

	@Test
	void testIsDraftEntity() {
		boolean isDraft = DraftUtils.isDraftEntity(draftEntity);
		assertEquals(true, isDraft, "The entity should be recognized as a draft entity");

		isDraft = DraftUtils.isDraftEntity(activeEntity);
		assertEquals(false, isDraft, "The entity should not be recognized as a draft entity");
	}
}
