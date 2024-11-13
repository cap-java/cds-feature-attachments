package com.sap.cds.feature.attachments.integrationtests.common;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.Struct;

@Component
class JsonToCapMapperTestHelper {

	@Autowired
	private ObjectMapper objectMapper;

	public CdsData mapResponseToSingleResult(String resultBody) throws Exception {
		return Struct.access(objectMapper.readValue(resultBody, HashMap.class)).as(CdsData.class);
	}

}
