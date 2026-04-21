/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.handlers.system;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.application.ApplicationPreparedEventContext;
import com.sap.cds.services.application.ApplicationStoppedEventContext;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ServiceName(ApplicationLifecycleService.DEFAULT_NAME)
public class SubscribeModelTenantsHandler implements EventHandler {

  @Autowired private CdsRuntime runtime;
  @Autowired private DeploymentService service;

  @On(event = ApplicationLifecycleService.EVENT_APPLICATION_PREPARED)
  public void subscribeMockTenants(ApplicationPreparedEventContext context) {
    var multiTenancy = runtime.getEnvironment().getCdsProperties().getMultiTenancy();

    if (Boolean.FALSE.equals(multiTenancy.getMock().isEnabled())) {
      return;
    }
    List<String> tenants = readMockedTenants();
    if (tenants.isEmpty()) {
      return;
    }
    if (!StringUtils.hasText(multiTenancy.getSidecar().getUrl())) {
      return;
    }

    tenants.forEach(this::subscribeTenant);
  }

  @On(event = ApplicationLifecycleService.EVENT_APPLICATION_STOPPED)
  public void unsubscribeMockTenants(ApplicationStoppedEventContext context) {
    var multiTenancy = runtime.getEnvironment().getCdsProperties().getMultiTenancy();

    if (Boolean.FALSE.equals(multiTenancy.getMock().isEnabled())) {
      return;
    }
    List<String> tenants = readMockedTenants();
    if (tenants.isEmpty()) {
      return;
    }

    tenants.forEach(this::unsubscribeTenant);
  }

  private void subscribeTenant(String tenant) {
    runtime
        .requestContext()
        .privilegedUser()
        .run(
            c -> {
              service.subscribe(
                  tenant,
                  new HashMap<>(Collections.singletonMap("subscribedSubdomain", "mt-" + tenant)));
            });
  }

  private void unsubscribeTenant(String tenant) {
    runtime
        .requestContext()
        .privilegedUser()
        .run(
            c -> {
              service.unsubscribe(tenant, Collections.emptyMap());
            });
  }

  private List<String> readMockedTenants() {
    return runtime
        .getEnvironment()
        .getCdsProperties()
        .getSecurity()
        .getMock()
        .getTenants()
        .values()
        .stream()
        .map(CdsProperties.Security.Mock.Tenant::getName)
        .collect(Collectors.toList());
  }
}
