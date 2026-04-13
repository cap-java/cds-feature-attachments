/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.ScanContentContext;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.ScanResult;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.TestService_;
import com.sap.cds.feature.attachments.service.MalwareScannerService;
import com.sap.cds.feature.attachments.service.malware.client.MalwareScanResultStatus;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Component;

@Component
@ServiceName(TestService_.CDS_NAME)
public class TestScanContentActionHandler implements EventHandler {

  private final CdsRuntime cdsRuntime;

  public TestScanContentActionHandler(CdsRuntime cdsRuntime) {
    this.cdsRuntime = cdsRuntime;
  }

  @On(event = ScanContentContext.CDS_NAME)
  public void onScanContent(ScanContentContext context) {
    MalwareScannerService scanner =
        cdsRuntime
            .getServiceCatalog()
            .getService(MalwareScannerService.class, MalwareScannerService.DEFAULT_NAME);

    MalwareScanResultStatus result =
        scanner.scanContent(new ByteArrayInputStream(context.getContent()));

    ScanResult scanResult = ScanResult.create();
    scanResult.setStatus(result.name());
    context.setResult(scanResult);
  }
}
