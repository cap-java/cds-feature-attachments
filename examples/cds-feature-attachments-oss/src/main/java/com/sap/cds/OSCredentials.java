package com.sap.cds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
/*import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;*/

@JsonIgnoreProperties(ignoreUnknown = true)
/*@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor*/
public class OSCredentials {

  private String url;

  private String baseTokenUrl;

  private String clientId;

  private String clientSecret;
}
