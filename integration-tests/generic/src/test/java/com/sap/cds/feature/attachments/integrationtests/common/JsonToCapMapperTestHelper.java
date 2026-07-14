/*
 * © 2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
class JsonToCapMapperTestHelper {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public CdsData mapResponseToSingleResult(String resultBody) throws Exception {
    return Struct.access(objectMapper.readValue(resultBody, HashMap.class)).as(CdsData.class);
  }
}
