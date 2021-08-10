package com.rtsp.media.exception;

/**
 * @class public class ResourceUnavailableException extends MediaException
 * @brief ResourceUnavailableException class
 */
public class ResourceUnavailableException extends MediaException {

    /**
     * Constructor.
     *
     * @param reason  the reason the exception occured.
     */
    public ResourceUnavailableException(String reason) {
        super(reason);
    }
}
