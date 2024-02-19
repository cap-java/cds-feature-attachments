package com.sap.cds.feature.attachments.service;

import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.Handler;

import java.io.InputStream;

/**
 * The interface defining the consumption API of the {@link AttachmentService}.
 * The {@link AttachmentService} is the connection to the storage for attachments, providing extensibility via {@link Handler}.
 */
public interface AttachmentService extends CqnService {

    /**
     * The {@link AttachmentService} wrapping the primary Attachment storage uses this name
     */
    String DEFAULT_NAME = "AttachmentService$Default";

    InputStream readAttachment(CdsReadEventContext context) throws AttachmentAccessException;

}
