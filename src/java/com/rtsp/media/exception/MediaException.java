package com.rtsp.media.exception;

/**
 * @class public class MediaException extends Exception
 * @brief MediaException class
 */
public class MediaException extends Exception {

    public MediaException() {
        super();
    }

    public MediaException(String reason) {
        super(reason);
    }
}
