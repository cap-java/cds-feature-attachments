package com.sap.cds.feature.attachments.handler.common;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;

public interface AttachmentsReader {

	List<CdsData> readAttachments(CdsModel model, CdsEntity entity, CqnDelete delete);

}
