/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.configuration.LockTimeoutConstant;
import com.sap.cds.feature.attachments.handler.common.model.NodeTree;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;

/**
	* The class {@link DefaultAttachmentsReader} is used to deep read attachments from the database
	* for a determined path from the given entity to the media entity.
	* The class uses the {@link AssociationCascader} to find the entity path.
	* <p>
	* The returned data is deep including the path structure to the media entity.
	*/
public class DefaultAttachmentsReader implements AttachmentsReader {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAttachmentsReader.class);
	private static final int LOCK_TIMEOUT_IN_SECONDS = LockTimeoutConstant.LOCK_TIMEOUT_IN_SECONDS;

	private final AssociationCascader cascader;
	private final PersistenceService persistence;

	public DefaultAttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
		this.cascader = cascader;
		this.persistence = persistence;
	}

	@Override
	public List<CdsData> readAttachments(CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
		logger.debug("Start reading attachments for entity {}", entity.getQualifiedName());

		var nodePath = cascader.findEntityPath(model, entity);
		var expandList = buildExpandList(nodePath);

		Select<?> select = !expandList.isEmpty() ? Select.from(statement.ref()).columns(expandList).lock(LOCK_TIMEOUT_IN_SECONDS) : Select.from(
				statement.ref()).columns(StructuredType::_all).lock(LOCK_TIMEOUT_IN_SECONDS);
		statement.where().ifPresent(select::where);

		var result = persistence.run(select);
		var cdsData = result.listOf(CdsData.class);
		logResultData(entity, cdsData);
		return cdsData;
	}

	private List<Expand<?>> buildExpandList(NodeTree root) {
		List<Expand<?>> expandResultList = new ArrayList<>();
		root.getChildren().forEach(child -> expandResultList.add(buildExpandFromTree(child)));

		return expandResultList;
	}

	private Expand<?> buildExpandFromTree(NodeTree node) {
		return node.getChildren().isEmpty() ? CQL.to(node.getIdentifier().associationName()).expand() : CQL.to(
				node.getIdentifier().associationName()).expand(node.getChildren().stream().map(this::buildExpandFromTree).toList());
	}

	private void logResultData(CdsEntity entity, List<CdsData> cdsData) {
		logger.debug("Read attachments for entity {}: lines {}", entity.getQualifiedName(), cdsData.size());
		if (logger.isTraceEnabled()) {
			cdsData.forEach(data -> logger.trace("Read attachment data: {}", data));
		}
	}

}
