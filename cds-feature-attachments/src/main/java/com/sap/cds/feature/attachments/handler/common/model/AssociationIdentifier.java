/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.common.model;

/**
 * This record is a simple data class that holds the association name and the full entity name.
 * 
 * @param associationName the association name
 * @param fullEntityName  the full entity name
 */
public record AssociationIdentifier(String associationName, String fullEntityName) {
}
