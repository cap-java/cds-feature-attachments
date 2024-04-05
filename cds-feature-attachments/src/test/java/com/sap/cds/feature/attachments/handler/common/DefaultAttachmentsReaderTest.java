package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.common.model.AssociationIdentifier;
import com.sap.cds.feature.attachments.handler.common.model.NodeTree;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;


class DefaultAttachmentsReaderTest {

	private DefaultAttachmentsReader cut;
	private AssociationCascader cascader;
	private PersistenceService persistenceService;
	private CdsEntity entity;
	private CdsModel model;
	private ArgumentCaptor<CqnSelect> selectArgumentCaptor;
	private Result result;

	@BeforeEach
	void setup() {
		cascader = mock(AssociationCascader.class);
		persistenceService = mock(PersistenceService.class);

		cut = new DefaultAttachmentsReader(cascader, persistenceService);

		entity = mock(CdsEntity.class);
		model = mock(CdsModel.class);
		selectArgumentCaptor = ArgumentCaptor.forClass(CqnSelect.class);
		result = mock(Result.class);
		when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);
	}

	@Test
	void correctSelectAndResultValue() {
		mockPathListAndEntity();
		var keys = buildDefaultKeyMap();
		var entityWithKeys = CQL.entity(RootTable_.CDS_NAME).matching(keys);
		CqnDelete deleteFromEntity = Delete.from(entityWithKeys).byId("test");
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntity);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatementWithWhereAndFilter((String) keys.get("ID")));
		assertThat(resultData).isEqualTo(data);
	}

	@Test
	void selectCorrectWithoutWhere() {
		mockPathListAndEntity();
		var keys = buildDefaultKeyMap();

		var entityWithKeys = CQL.entity(RootTable_.CDS_NAME).matching(keys);
		CqnDelete deleteFromEntityWithoutWhere = Delete.from(entityWithKeys);
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntityWithoutWhere);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatementWithFilter((String) keys.get("ID")));
		assertThat(resultData).isEqualTo(data);
	}

	@Test
	void selectCorrectWithoutFilter() {
		mockPathListAndEntity();
		CqnDelete deleteFromEntityWithoutFilter = Delete.from(RootTable_.CDS_NAME).byId("test");
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntityWithoutFilter);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatementWithWhere());
		assertThat(resultData).isEqualTo(data);
	}

	@Test
	void selectCorrectWithoutWhereAndFilter() {
		mockPathListAndEntity();
		CqnDelete deleteFromEntityWithoutWhereAndFilter = Delete.from(RootTable_.CDS_NAME);
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntityWithoutWhereAndFilter);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatement());
		assertThat(resultData).isEqualTo(data);
	}

	@Test
	void selectCorrectWithWhereAndFilterForNonRootTable() {
		mockPathListAndEntity(Items_.CDS_NAME);
		var keys = buildDefaultKeyMap();
		var entityWithKeys = CQL.entity(Items_.CDS_NAME).matching(keys);
		CqnDelete deleteFromEntity = Delete.from(entityWithKeys).byId("test");
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntity);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatementForItemsWithWhereAndFilter((String) keys.get("ID")));
		assertThat(resultData).isEqualTo(data);
	}

	@Test
	void selectCorrectWithWhereAndFilterForAttachments() {
		mockPathListAndEntity(Attachment_.CDS_NAME);
		var keys = buildDefaultKeyMap();
		var entityWithKeys = CQL.entity(Attachment_.CDS_NAME).matching(keys);
		CqnDelete deleteFromEntity = Delete.from(entityWithKeys).byId("test");
		List<CdsData> data = List.of(CdsData.create());
		when(result.listOf(CdsData.class)).thenReturn(data);

		var resultData = cut.readAttachments(model, entity, deleteFromEntity);

		verify(persistenceService).run(selectArgumentCaptor.capture());
		assertThat(selectArgumentCaptor.getValue()).hasToString(getExpectedSelectStatementForAttachmentsWithWhereAndFilter((String) keys.get("ID")));
		assertThat(resultData).isEqualTo(data);
	}

	private HashMap<String, Object> buildDefaultKeyMap() {
		var keys = new HashMap<String, Object>();
		keys.put("IsActiveEntity", true);
		keys.put("ID", UUID.randomUUID().toString());
		return keys;
	}

	private void mockPathListAndEntity() {
		mockPathListAndEntity(RootTable_.CDS_NAME);
	}

	private void mockPathListAndEntity(String entityName) {
		var pathList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var rootPath = new LinkedList<AssociationIdentifier>();
		rootPath.add(new AssociationIdentifier("", RootTable_.CDS_NAME));
		rootPath.add(new AssociationIdentifier("items", Items_.CDS_NAME));
		rootPath.add(new AssociationIdentifier("attachments", Attachment_.CDS_NAME));
		pathList.add(rootPath);
		var nodeTree = new NodeTree(new AssociationIdentifier("", entityName));
		pathList.forEach(nodeTree::addPath);
		when(cascader.findEntityPath(model, entity)).thenReturn(nodeTree);
		when(entity.getQualifiedName()).thenReturn(entityName);
	}

	private String getExpectedSelectStatementWithWhereAndFilter(String id) {
		var select = """
				{"SELECT":{"from":{"ref":[{"id":"unit.test.TestService.RootTable",
				           "where":[{"ref":["IsActiveEntity"]},"=",{"val":true},
				             "and",{"ref":["ID"]},"=",{"val":"%s"}]}]},
				           "columns":[{"ref":["items"],
				            "expand":[{"ref":["attachments"],
				            "expand":["*"]}]}],
				           "where":[{"ref":["$key"]},"=",{"val":"test"}]}}
				""".formatted(id);
		return removeSpaceInString(select);
	}

	private String getExpectedSelectStatementForItemsWithWhereAndFilter(String id) {
		var select = """
				{"SELECT":{"from":{"ref":[{"id":"unit.test.TestService.Items",
				           "where":[{"ref":["IsActiveEntity"]},"=",{"val":true},
				             "and",{"ref":["ID"]},"=",{"val":"%s"}]}]},
				           "columns":[{"ref":["attachments"],
				           "expand":["*"]}],
				           "where":[{"ref":["$key"]},"=",{"val":"test"}]}}
				""".formatted(id);
		return removeSpaceInString(select);
	}

	private String getExpectedSelectStatementForAttachmentsWithWhereAndFilter(String id) {
		var select = """
				{"SELECT":{"from":{"ref":[{"id":"unit.test.Attachment",
				           "where":[{"ref":["IsActiveEntity"]},"=",{"val":true},
				             "and",{"ref":["ID"]},"=",{"val":"%s"}]}]},
				           "columns":["*"],
				           "where":[{"ref":["$key"]},"=",{"val":"test"}]}}
				""".formatted(id);
		return removeSpaceInString(select);
	}

	private String getExpectedSelectStatementWithFilter(String id) {
		var select = """
				{"SELECT":{"from":{"ref":[{"id":"unit.test.TestService.RootTable",
				           "where":[{"ref":["IsActiveEntity"]},"=",{"val":true},
				             "and",{"ref":["ID"]},"=",{"val":"%s"}]}]},
				           "columns":[{"ref":["items"],
				           "expand":[{"ref":["attachments"],
				           "expand":["*"]}]}]}}
				""".formatted(id);
		return removeSpaceInString(select);
	}

	private String getExpectedSelectStatementWithWhere() {
		var select = """
				{"SELECT":{"from":{"ref":["unit.test.TestService.RootTable"]},
				          "columns":[{"ref":["items"],
				          "expand":[{"ref":["attachments"],
				          "expand":["*"]}]}],
				          "where":[{"ref":["$key"]},"=",{"val":"test"}]}}
				""";
		return removeSpaceInString(select);
	}

	private String getExpectedSelectStatement() {
		var select = """
				{"SELECT":{"from":{"ref":["unit.test.TestService.RootTable"]},
				          "columns":[{"ref":["items"],
				          "expand":[{"ref":["attachments"],
				          "expand":["*"]}]}]}}
				""";
		return removeSpaceInString(select);
	}

	private String removeSpaceInString(String input) {
		return input.replace("\n", "").replace("\t", "").replace(" ", "");
	}

}
