/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.servicehandler;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

/**
	* The {@link DeletionUserInfo} is used to store the user information of the user which triggers the deletion of an attachment.
	*/
public interface DeletionUserInfo extends CdsData {

	String NAME = "name";

	static DeletionUserInfo create() {
		return Struct.create(DeletionUserInfo.class);
	}

	String getName();

	void setName(String id);

}
