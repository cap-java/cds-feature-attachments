/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CQL modifier that transforms entity references for draft/active entity handling.
 *
 * <p>This modifier flattens complex entity references by removing nested references and creating a
 * new CQN statement for the specified {@code fullEntityName}. It performs the following
 * transformations:
 *
 * <ul>
 *   <li>Removes nested references and creates a new entity reference for {@code fullEntityName}
 *   <li>Preserves the filter from the last segment of the original {@link CqnStructuredTypeRef}
 *   <li>Adds an {@code IsActiveEntity} filter with the specified boolean value
 * </ul>
 *
 * <p>This is primarily used in draft service scenarios to transform queries between draft entities
 * (IsActiveEntity = false) and active entities (IsActiveEntity = true).
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
    logger.debug(
        "Modifying ref {} with isActiveEntity: {} and fullEntityName: {}",
        rootSegment,
        isActiveEntity,
        fullEntityName);

    // Get the filter from the last segment:
    // Get the last segment with targetSegment, then an Optional<CqnPredicate> with filter()
    // which is then unwrapped to CqnPredicate or null by orElse(null).
    CqnPredicate lastSegmentFilter = original.targetSegment().filter().orElse(null);

    // Create an IsActiveEntity filter
    CqnPredicate isActiveEntityFilter = CQL.get(Drafts.IS_ACTIVE_ENTITY).eq(isActiveEntity);

    // Combine with original filter if it exists
    CqnPredicate combinedFilter =
        lastSegmentFilter != null
            ? CQL.and(lastSegmentFilter, isActiveEntityFilter)
            : isActiveEntityFilter;

    // Apply any additional modifications (like replacing other IsActiveEntity references)
    // This calls the comparison() method below for each comparison in the filter
    CqnPredicate modifiedFilter = CQL.copy(combinedFilter, this);

    // Create a new entity reference with the modified filter
    return CQL.entity(fullEntityName).filter(modifiedFilter).asRef();
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
