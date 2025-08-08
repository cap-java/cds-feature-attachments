/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.ql.CQL;
import com.sap.cds.ql.RefBuilder;
import com.sap.cds.ql.RefBuilder.RefSegment;
import com.sap.cds.ql.StructuredTypeRef;
import com.sap.cds.ql.Value;
import com.sap.cds.ql.cqn.CqnComparisonPredicate.Operator;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.services.draft.Drafts;

/**
 * The class is used to modify the following values in a given {@link CqnStructuredTypeRef}:
 * <ul>
 * <li>{@code isActiveEntity}</li>
 * <li>{@code fullEntityName}</li>
 * </ul>
 */
class ActiveEntityModifier implements Modifier {

	private static final Logger logger = LoggerFactory.getLogger(ActiveEntityModifier.class);

	private final boolean isActiveEntity;
	private final String fullEntityName;

	ActiveEntityModifier(boolean isActiveEntity, String fullEntityName) {
		this.isActiveEntity = isActiveEntity;
		this.fullEntityName = fullEntityName;
	}

	@Override
	public CqnStructuredTypeRef ref(CqnStructuredTypeRef original) {
		RefBuilder<StructuredTypeRef> ref = CQL.copy(original);
		RefSegment rootSegment = ref.rootSegment();
		logger.debug("Modifying ref {} with isActiveEntity: {} and fullEntityName: {}", rootSegment, isActiveEntity,
				fullEntityName);
		rootSegment.id(fullEntityName);

		Modifier modifier = new ActiveEntityModifier(isActiveEntity, fullEntityName);
		for (RefSegment segment : ref.segments()) {
			segment.filter(segment.filter().map(filter -> CQL.copy(filter, modifier)).orElse(null));
		}
		return ref.build();
	}

	@Override
	public CqnPredicate comparison(Value<?> lhs, Operator op, Value<?> rhs) {
		Value<?> rhsNew = rhs;
		Value<?> lhsNew = lhs;
		if (lhs.isRef() && Drafts.IS_ACTIVE_ENTITY.equals(lhs.asRef().lastSegment())) {
			rhsNew = CQL.constant(isActiveEntity);
		}
		if (rhs.isRef() && Drafts.IS_ACTIVE_ENTITY.equals(rhs.asRef().lastSegment())) {
			lhsNew = CQL.constant(isActiveEntity);
		}
		return CQL.comparison(lhsNew, op, rhsNew);
	}

}
