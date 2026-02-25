/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.media;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.validation.AttachmentValidationHelper;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MediaTypeResolver {
  private static final String CONTENT_ELEMENT = "content";
  private static final String ACCEPTABLE_MEDIA_TYPES_ANNOTATION = "Core.AcceptableMediaTypes";
  private static final TypeReference<List<String>> STRING_LIST_TYPE_REF = new TypeReference<>() {
  };
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Resolves the acceptable media (MIME) types for the given {@link CdsEntity}.
   *
   * <p>
   * The method behaves differently depending on whether the provided entity
   * itself is a media entity or a root entity containing compositions:
   *
   * <p>
   * If no media entities are found (neither the root nor its composition
   * targets), an empty map is returned.
   *
   * @param entity the CDS entity to inspect (root or media entity)
   * @return a map of entity qualified names to their allowed media types;
   *         empty if no media entities are found
   */
  public static Map<String, List<String>> getAcceptableMediaTypesFromEntity(CdsEntity entity) {
    // If this entity is a media entity
    if (ApplicationHandlerHelper.isMediaEntity(entity)) {
      return Map.of(entity.getQualifiedName(), fetchAcceptableMediaTypes(entity));
    }

    // If it's not a mediaEntity, if it's a root entity
    Map<String, List<String>> result = new HashMap<>();
    entity
        .elements()
        .filter(e -> e.getType().isAssociation())
        .map(e -> e.getType().as(CdsAssociationType.class))
        .filter(CdsAssociationType::isComposition)
        .forEach(
            association -> {
              CdsEntity target = association.getTarget();
              if (target != null && ApplicationHandlerHelper.isMediaEntity(target)) {
                result.put(target.getQualifiedName(), fetchAcceptableMediaTypes(target));
              }
            });

    return result;
  }

  private static List<String> fetchAcceptableMediaTypes(CdsEntity entity) {
    return getAcceptableMediaTypesAnnotation(entity)
        .map(CdsAnnotation::getValue)
        .map(value -> objectMapper.convertValue(value, STRING_LIST_TYPE_REF))
        .orElse(AttachmentValidationHelper.WILDCARD_MEDIA_TYPE);
  }

  public static Optional<CdsAnnotation<Object>> getAcceptableMediaTypesAnnotation(
      CdsEntity entity) {
    return Optional.ofNullable(entity.getElement(CONTENT_ELEMENT))
        .flatMap(element -> element.findAnnotation(ACCEPTABLE_MEDIA_TYPES_ANNOTATION));
  }

  private MediaTypeResolver() {
    // to prevent instantiation
  }
}
