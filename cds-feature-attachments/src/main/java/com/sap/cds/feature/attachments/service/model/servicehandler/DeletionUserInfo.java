package com.sap.cds.feature.attachments.service.model.servicehandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

public interface DeletionUserInfo extends CdsData {

	String ID = "id";
	String NAME = "name";
	String TENANT = "tenant";
	String IS_SYSTEM_USER = "isSystemUser";
	String IS_INTERNAL_USER = "isInternalUser";
	String IS_AUTHENTICATED = "isAuthenticated";
	String IS_PRIVILEGED = "isPrivileged";
	String ROLES = "roles";
	String ATTRIBUTES = "attributes";
	String ADDITIONAL_ATTRIBUTED = "additionalAttributes";

	String getId();

	void setId(String id);

	String getName();

	void setName(String id);

	String getTenant();

	void setTenant(String id);

	boolean getIsSystemUser();

	void setIsSystemUser(boolean isSystemUser);

	boolean getIsInternalUser();

	void setIsInternalUser(boolean isInternalUser);

	boolean getIsAuthenticated();

	void setIsAuthenticated(boolean isAuthenticated);

	boolean getIsPrivileged();

	void setIsPrivileged(boolean isPrivileged);

	Set<String> getRoles();

	void setRoles(Set<String> roles);

	Map<String, List<String>> getAttributes();

	void setAttributes(Map<String, List<String>> attributes);

	Map<String, Object> getAdditionalAttributes();

	void setAdditionalAttributes(Map<String, Object> additionalAttributed);

	static DeletionUserInfo create() {
		return Struct.create(DeletionUserInfo.class);
	}

}
