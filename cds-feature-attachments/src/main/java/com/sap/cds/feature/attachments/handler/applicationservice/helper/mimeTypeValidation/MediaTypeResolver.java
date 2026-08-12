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
  private static final String CONTENT_SUFFIX = "_" + CONTENT_ELEMENT;
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
      fetchAcceptableMediaTypes(mediaEntity).ifPresent(types -> result.put(entityName, types));
      fetchInlineAcceptableMediaTypes(mediaEntity, entityName).forEach(result::put);
    }

    return result;
  }

  private static Optional<List<String>> fetchAcceptableMediaTypes(CdsEntity entity) {
    return getAcceptableMediaTypesAnnotation(entity)
        .map(CdsAnnotation::getValue)
        .map(value -> objectMapper.convertValue(value, STRING_LIST_TYPE_REF));
  }

  private static Map<String, List<String>> fetchInlineAcceptableMediaTypes(
      CdsEntity entity, String entityName) {
    Map<String, List<String>> result = new HashMap<>();
    entity
        .elements()
        .filter(e -> e.getName().endsWith(CONTENT_SUFFIX))
        .forEach(
            e ->
                e.findAnnotation(ACCEPTABLE_MEDIA_TYPES_ANNOTATION)
                    .map(a -> objectMapper.convertValue(a.getValue(), STRING_LIST_TYPE_REF))
                    .ifPresent(
                        types -> {
                          String prefix =
                              e.getName()
                                  .substring(0, e.getName().length() - CONTENT_SUFFIX.length());
                          result.put(entityName + "#" + prefix, types);
                        }));
    return result;
  }

  public static Optional<CdsAnnotation<Object>> getAcceptableMediaTypesAnnotation(
      CdsEntity entity) {
    return entity
        .findElement(CONTENT_ELEMENT)
        .flatMap(element -> element.findAnnotation(ACCEPTABLE_MEDIA_TYPES_ANNOTATION));
  }

  private MediaTypeResolver() {
    // to prevent instantiation
  }
}
