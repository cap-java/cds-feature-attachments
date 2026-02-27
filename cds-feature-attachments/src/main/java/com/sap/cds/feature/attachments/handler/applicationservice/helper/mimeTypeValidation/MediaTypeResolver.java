/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MediaTypeResolver {
  private static final String CONTENT_ELEMENT = "content";
  private static final String ACCEPTABLE_MEDIA_TYPES_ANNOTATION = "Core.AcceptableMediaTypes";
  private static final TypeReference<List<String>> STRING_LIST_TYPE_REF = new TypeReference<>() {};
  private static AssociationCascader cascader = new AssociationCascader();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Resolves the acceptable media (MIME) types for the given {@link CdsEntity}.
   *
   * <p>The method behaves differently depending on whether the provided entity itself is a media
   * entity or a root entity containing compositions:
   *
   * <p>If no media entities are found (neither the root nor its composition targets), an empty map
   * is returned.
   *
   * @param entity the CDS entity to inspect (root or media entity)
   * @return a map of entity qualified names to their allowed media types; empty if no media
   *     entities are found
   */
  static void setCascader(AssociationCascader testCascader) {
    cascader = testCascader;
  }

  public static Map<String, List<String>> getAcceptableMediaTypesFromEntity(
      CdsEntity entity, CdsModel model) {
    Map<String, List<String>> result = new HashMap<>();
    List<String> mediaEntityNames = cascader.findMediaEntityNames(model, entity);
    if (mediaEntityNames.isEmpty()) {
      return result;
    }
    for (String entityName : mediaEntityNames) {
      CdsEntity mediaEntity = model.getEntity(entityName);
      result.put(entityName, fetchAcceptableMediaTypes(mediaEntity));
    }

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
