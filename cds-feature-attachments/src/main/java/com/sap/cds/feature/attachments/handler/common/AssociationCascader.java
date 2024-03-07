package com.sap.cds.feature.attachments.handler.common;

import java.util.LinkedList;
import java.util.List;

import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;

public interface AssociationCascader {

	List<LinkedList<AssociationIdentifier>> findEntityPath(CdsModel model, CdsEntity entity, Filter filter);

}
