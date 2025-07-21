/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.common;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.persistence.PersistenceService;

/**
 * The class {@link AttachmentsReader} is used to deep read attachments from the
 * database for a determined path from the given entity to the media entity. The
 * class uses the {@link AssociationCascader} to find the entity path.
 * <p>
 * The returned data is deep including the path structure to the media entity.
 */
public class AttachmentsReader {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentsReader.class);

	private final AssociationCascader cascader;
	private final PersistenceService persistence;

	public AttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
		this.cascader = requireNonNull(cascader, "cascader must not be null");
		this.persistence = requireNonNull(persistence, "persistence must not be null");
	}

	public List<Attachments> readAttachments(CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
		logger.debug("Start reading attachments for entity {}", entity.getQualifiedName());

		NodeTree nodePath = cascader.findEntityPath(model, entity);
		List<Expand<?>> expandList = buildExpandList(nodePath);

		Select<?> select = !expandList.isEmpty() ? Select.from(statement.ref()).columns(expandList)
				: Select.from(statement.ref()).columns(StructuredType::_all);
		statement.where().ifPresent(select::where);

		Result result = persistence.run(select);
		List<Attachments> attachments = result.listOf(Attachments.class);
		logResultData(entity, attachments);
		return attachments;
	}

	private List<Expand<?>> buildExpandList(NodeTree root) {
		List<Expand<?>> expandResultList = new ArrayList<>();
		root.getChildren().forEach(child -> expandResultList.add(buildExpandFromTree(child)));

		return expandResultList;
	}

	private Expand<?> buildExpandFromTree(NodeTree node) {
		return node.getChildren().isEmpty() ? CQL.to(node.getIdentifier().associationName()).expand()
				: CQL.to(node.getIdentifier().associationName())
						.expand(node.getChildren().stream().map(this::buildExpandFromTree).toList());
	}

	private static void logResultData(CdsEntity entity, List<Attachments> attachments) {
		logger.debug("Read attachments for entity {}: lines {}", entity.getQualifiedName(), attachments.size());
		if (logger.isTraceEnabled()) {
			attachments.forEach(data -> logger.trace("Read attachment data: {}", data));
		}
	}

}
