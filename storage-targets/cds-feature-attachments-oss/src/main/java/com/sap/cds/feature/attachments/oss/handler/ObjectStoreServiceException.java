package com.sap.cds.feature.attachments.oss.handler;

public class ObjectStoreServiceException extends RuntimeException {
    public ObjectStoreServiceException(String message) {
        super(message);
    }
    public ObjectStoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
