package com.sap.cds.feature.attachments.handler.applicationservice;

import com.sap.cds.services.ServiceException;

import java.io.IOException;

public class AttachmentContentException extends IOException {
    private final ServiceException serviceException;

    public AttachmentContentException(String message, ServiceException cause) {
        super(message, cause);
        this.serviceException = cause;
    }

    public ServiceException getServiceException() {
        return serviceException;
    }
}
