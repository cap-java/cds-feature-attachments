/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.utils.model.CdsModelUtils;

/**
 * The class {@link ApplicationHandlerHelper} provides helper methods for the attachment application handlers.
 */
public final class ApplicationHandlerHelper {

	/**
	 * A filter for media content fields. The filter checks if the entity is a media entity and if the element has the
	 * annotation "Core.MediaType".
	 */
	public static final Filter MEDIA_CONTENT_FILTER = (path, element, type) -> isMediaEntity(path.target().type())
			&& hasElementAnnotation(element, ModelConstants.ANNOTATION_CORE_MEDIA_TYPE);

	/**
	 * Checks if the data contains a content field.
	 * 
	 * @param entity The {@link CdsEntity} to check
	 * @param data   The data to check
	 * @return <code>true</code> if the data contains a content field, <code>false</code> otherwise
	 */
	public static boolean noContentFieldInData(CdsEntity entity, List<CdsData> data) {
		var isIncluded = new AtomicBoolean();
		Validator validator = (path, element, value) -> isIncluded.set(true);

		callValidator(entity, data, MEDIA_CONTENT_FILTER, validator);
		return !isIncluded.get();
	}

	public static void callProcessor(CdsEntity entity, List<CdsData> data, Filter filter, Converter converter) {
		CdsDataProcessor.create().addConverter(filter, converter).process(data, entity);
	}

	public static void callValidator(CdsEntity entity, List<CdsData> data, Filter filter, Validator validator) {
		CdsDataProcessor.create().addValidator(filter, validator).process(data, entity);
	}

	/**
	 * Checks if the entity is a media entity. A media entity is an entity that is annotated with the annotation
	 * "_is_media_data".
	 * 
	 * @param baseEntity The entity to check
	 * @return <code>true</code> if the entity is a media entity, <code>false</code> otherwise
	 */
	public static boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

	public static boolean hasElementAnnotation(CdsElement element, String annotation) {
		return element.findAnnotation(annotation).isPresent();
	}

	public static boolean doesContentIdExistsBefore(Map<?, Object> existingData) {
		return Objects.nonNull(existingData.get(Attachments.CONTENT_ID));
	}

	public static List<CdsData> condenseData(List<CdsData> data, CdsEntity entity) {
		var resultList = new ArrayList<CdsData>();

		Validator validator = (path, element, value) -> resultList.add(CdsData.create(path.target().values()));

		callValidator(entity, data, MEDIA_CONTENT_FILTER, validator);
		return resultList;
	}

	public static boolean areKeysInData(Map<String, Object> keys, CdsData data) {
		return keys.entrySet().stream().allMatch(entry -> {
			var keyInData = data.get(entry.getKey());
			return Objects.nonNull(keyInData) && keyInData.equals(entry.getValue());
		});
	}

	public static Map<String, Object> removeDraftKeys(Map<String, Object> keys) {
		var keyMap = new HashMap<>(keys);
		keyMap.entrySet().removeIf(entry -> isDraftActiveEntityField(entry.getKey()));
		return keyMap;
	}

	public static CdsEntity getBaseEntity(CdsModel model, CdsEntity entity) {
		var entityResultOptional = entity.query().map(q -> CdsModelUtils.getEntityPath(q, model).rootEntity());
		return entityResultOptional.orElseGet(() -> model.findEntity(entity.getQualifiedName()).orElseThrow());
	}

	private static boolean isDraftActiveEntityField(String key) {
		return key.equals(Drafts.IS_ACTIVE_ENTITY);
	}

	private ApplicationHandlerHelper() {
		// avoid instantiation
	}

}
