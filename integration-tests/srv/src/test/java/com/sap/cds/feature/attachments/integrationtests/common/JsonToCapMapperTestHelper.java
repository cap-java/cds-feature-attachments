package com.sap.cds.feature.attachments.integrationtests.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sap.cds.CdsData;
import com.sap.cds.Struct;

@Component
class JsonToCapMapperTestHelper {

	private static final Logger logger = LoggerFactory.getLogger(JsonToCapMapperTestHelper.class);

	@Autowired
	private ObjectMapper objectMapper;

	public CdsData mapResponseToSingleResult(String resultBody)	throws Exception {
		var map = new HashMap<String, Object>();
		return Struct.access(objectMapper.readValue(resultBody, map.getClass())).as(CdsData.class);
	}

	public <T> List<T> mapJsonToResultList(Class<T> resultType, JsonNode dataNode) throws Exception {
		List<T> resultList = new ArrayList<>();

		if (Objects.nonNull(dataNode)) {
			if (dataNode.isObject()) {
				addElementToResultList(resultType, dataNode, resultList);
			}

			if (dataNode.isArray()) {
				ArrayNode arrayNode = (ArrayNode) dataNode;

				for (int i = 0; i < arrayNode.size(); i++) {
					addElementToResultList(resultType, arrayNode.get(i), resultList);
				}
			}
		}

		return resultList;
	}

	private <T> void addElementToResultList(Class<T> resultType, JsonNode dataNode, List<T> resultList)	throws Exception {
		String node = dataNode.toString();

		Map<String, Object> resultMap = objectMapper.readValue(node, HashMap.class);

		mapListEntries(Collections.singletonList(resultMap));

		T resultEntry = Struct.access(resultMap).as(resultType);
		resultList.add(resultEntry);
	}

	private void mapListEntries(List<Map<String, Object>> resultMapList) {

		if (Objects.isNull(resultMapList)) {
			return;
		}

		for (Map<String, Object> resultMap : resultMapList) {
			for (Map.Entry<String, Object> resultEntry : resultMap.entrySet()) {
				if (resultEntry.getValue() instanceof LinkedHashMap) {
					List<Map<String, Object>> newValue = (List<Map<String, Object>>) ((LinkedHashMap<?, ?>) resultEntry.getValue()).get("results");

					if (Objects.isNull(newValue)) {
						continue;
					}

					mapListEntries(newValue);
					resultEntry.setValue(newValue);
				}
			}
		}
	}

	private static class CdsDataList extends ArrayList<HashMap<String, Object>> {
	}

}
