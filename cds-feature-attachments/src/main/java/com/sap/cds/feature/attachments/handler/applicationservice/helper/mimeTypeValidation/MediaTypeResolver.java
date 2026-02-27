/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static Map<String, List<String>> getAcceptableMediaTypesFromEntity(
      CdsModel model, List<String> mediaEntityNames) {
    Map<String, List<String>> result = new HashMap<>();
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
