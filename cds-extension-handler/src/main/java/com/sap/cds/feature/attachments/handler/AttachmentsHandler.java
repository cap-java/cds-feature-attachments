package com.sap.cds.feature.attachments.handler;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler implements EventHandler {

    private final AttachmentService attachmentService;

    public AttachmentsHandler(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @After
    @HandlerOrder(HandlerOrder.EARLY)
    void readAttachments(CdsReadEventContext context, List<CdsData> data) {

    }

    @On
    @HandlerOrder(HandlerOrder.EARLY)
    public void uploadAttachment(CdsUpdateEventContext context, List<CdsData> cdsData) {

    }


}
