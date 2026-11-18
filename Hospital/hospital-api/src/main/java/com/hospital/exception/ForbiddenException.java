package com.hospital.exception;

/**
 * 越权访问：当前登录身份无权执行该操作。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
