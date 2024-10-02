package com.sap.cds.feature.attachments.service.model.servicehandler;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

/**
	* The {@link DeletionUserInfo} is used to store the user information of the user which triggers the deletion of an attachment.
	*/
public interface DeletionUserInfo extends CdsData {

	String ID = "id";
	String NAME = "name";
	String TENANT = "tenant";

	static DeletionUserInfo create() {
		return Struct.create(DeletionUserInfo.class);
	}

	String getId();

	void setId(String id);

	String getName();

	void setName(String id);

	String getTenant();

	void setTenant(String id);

}
