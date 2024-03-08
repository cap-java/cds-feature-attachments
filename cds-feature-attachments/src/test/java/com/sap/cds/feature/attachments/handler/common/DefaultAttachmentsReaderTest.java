package com.sap.cds.feature.attachments.handler.common;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;

class DefaultAttachmentsReaderTest {

	private DefaultAttachmentsReader cut;
	private AssociationCascader cascader;
	private CdsEntity entity;

	@BeforeEach
	void setup() {
		cascader = mock(AssociationCascader.class);

		cut = new DefaultAttachmentsReader(cascader);

		entity = mock(CdsEntity.class);
	}

	@Test
	void correctHandlingOfEntities() {
		var pathList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var rootPath = new LinkedList<AssociationIdentifier>();
		rootPath.add(new AssociationIdentifier("", RootTable_.CDS_NAME, false));
		rootPath.add(new AssociationIdentifier("items", "ITEM", false));
		rootPath.add(new AssociationIdentifier("attachments", "ATTACHMENTS", true));
		pathList.add(rootPath);
		when(cascader.findEntityPath(any(), any())).thenReturn(pathList);
		when(entity.getQualifiedName()).thenReturn(RootTable_.CDS_NAME);
		CqnDelete delete = Delete.from(RootTable_.class).byId("test");

		var keys = new HashMap<String, Object>();
		keys.put("IsActiveEntity", true);
		keys.put("ID", UUID.randomUUID().toString());

		var entityWithKeys = CQL.entity(RootTable_.CDS_NAME).matching(keys);
		CqnDelete deleteFromEntity = Delete.from(entityWithKeys).byId("test");

		cut.readAttachments(mock(CdsModel.class), entity, deleteFromEntity);

	}


}