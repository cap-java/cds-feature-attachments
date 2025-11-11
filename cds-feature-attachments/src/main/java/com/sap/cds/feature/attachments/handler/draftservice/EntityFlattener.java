/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.ql.CQL;
import com.sap.cds.ql.RefBuilder;
import com.sap.cds.ql.RefBuilder.RefSegment;
import com.sap.cds.ql.StructuredTypeRef;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
import com.sap.cds.ql.cqn.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modifier that flattens nested entity references into direct entity references.
 *
 * <p>This modifier transforms nested references (e.g., Books → covers) into a single entity
 * reference (e.g., Books.covers_drafts) while preserving filter conditions from the target segment.
 */
class EntityFlattener implements Modifier {

  private static final Logger logger = LoggerFactory.getLogger(EntityFlattener.class);

  EntityFlattener() {}

  @Override
  public CqnStructuredTypeRef ref(CqnStructuredTypeRef original) {

    RefBuilder<StructuredTypeRef> ref = CQL.copy(original);
    RefSegment rootSegment = ref.rootSegment();

    // Flatten the query when it's nested, e.g. Books -> Chapters -> Chapters.images
    if (ref.segments().size() > 1) {
      logger.debug("Removing nested segments for ref {}", rootSegment);
    } else {
      // No nested segments, return as is
      return original;
    }

    // Get the filter from the last segment before removing segments
    RefSegment lastSegment = ref.segments().get(ref.segments().size() - 1);
    CqnPredicate lastFilter = null;
    if (lastSegment.filter().isPresent()) {
      Modifier modifier = new EntityFlattener();
      lastFilter = CQL.copy(lastSegment.filter().get(), modifier);
    }

    while (ref.segments().size() > 1) {
      ref.segments().remove(ref.segments().size() - 1);
    }

    // Apply the filter to the root segment if there was a filter on the last segment
    if (lastFilter != null) {
      rootSegment.filter(lastFilter);
    }

    // Create a direct reference to the target entity with the modified filter
    return ref.build();
  }
}
