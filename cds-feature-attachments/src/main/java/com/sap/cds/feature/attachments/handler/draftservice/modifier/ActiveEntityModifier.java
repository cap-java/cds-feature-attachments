package com.sap.cds.feature.attachments.handler.draftservice.modifier;

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
	* The class {@link ActiveEntityModifier} is used to modify the following values in a given ref: <br>
	* - {@code isActiveEntity} <br>
	* - {@code fullEntityName}
	*/
public class ActiveEntityModifier implements Modifier {

	private static final Logger logger = LoggerFactory.getLogger(ActiveEntityModifier.class);

	private final boolean isActiveEntity;
	private final String fullEntityName;

	public ActiveEntityModifier(boolean isActiveEntity, String fullEntityName) {
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

		for (RefSegment segment : ref.segments()) {
			if (segment.filter().isPresent()) {
				segment.filter(CQL.copy(segment.filter().orElseThrow(), new ActiveEntityModifier(isActiveEntity, fullEntityName)));
			}
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

