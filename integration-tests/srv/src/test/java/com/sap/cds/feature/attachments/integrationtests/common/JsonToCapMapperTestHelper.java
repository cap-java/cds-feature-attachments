/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class JsonToCapMapperTestHelper {

  @Autowired private ObjectMapper objectMapper;

  public CdsData mapResponseToSingleResult(String resultBody) throws Exception {
    return Struct.access(objectMapper.readValue(resultBody, HashMap.class)).as(CdsData.class);
  }
}
