package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.feature.attachments.handler.common.model.NodeTree;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;

public interface AssociationCascader {

	NodeTree findEntityPath(CdsModel model, CdsEntity entity);

}
