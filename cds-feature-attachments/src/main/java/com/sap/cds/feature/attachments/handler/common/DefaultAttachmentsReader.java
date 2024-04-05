package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.model.NodeTree;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;

public class DefaultAttachmentsReader implements AttachmentsReader {

	private final AssociationCascader cascader;
	private final PersistenceService persistence;

	public DefaultAttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
		this.cascader = cascader;
		this.persistence = persistence;
	}

	@Override
	public List<CdsData> readAttachments(CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
		return getData(model, entity, statement);
	}

	private List<CdsData> getData(CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
		var nodePath = cascader.findEntityPath(model, entity);
		var expandList = buildExpandList(nodePath);

		Select<?> select = !expandList.isEmpty() ? Select.from(statement.ref())
																																															.columns(expandList) : Select.from(statement.ref())
																																																																								.columns(StructuredType::_all);
		statement.where().ifPresent(select::where);

		var result = persistence.run(select);
		return result.listOf(CdsData.class);
	}

	private List<Expand<?>> buildExpandList(NodeTree root) {
		List<Expand<?>> expandResultList = new ArrayList<>();
		root.getChildren().forEach(child -> expandResultList.add(buildExpandFromTree(child)));

		return expandResultList;
	}

	private Expand<?> buildExpandFromTree(NodeTree node) {
		return node.getChildren().isEmpty() ? CQL.to(node.getIdentifier().associationName())
																																										.expand() : CQL.to(node.getIdentifier().associationName())
																																																								.expand(node.getChildren().stream()
																																																																		.map(this::buildExpandFromTree).toList());
	}

}
