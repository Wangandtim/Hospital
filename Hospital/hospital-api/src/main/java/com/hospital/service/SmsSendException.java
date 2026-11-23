package com.hospital.service;

/**
 * 短信发送失败。message 是可展示给调用方的可读文案，不包含厂商原始错误与密钥。
 */
public class SmsSendException extends RuntimeException {

    public SmsSendException(String message) {
        super(message);
    }
}
