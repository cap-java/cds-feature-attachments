/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import com.sap.cds.services.EventContext;

public record EventContextHolder(String event, EventContext context) {
}
