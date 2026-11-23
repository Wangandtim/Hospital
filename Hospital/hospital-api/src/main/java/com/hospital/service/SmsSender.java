package com.hospital.service;

/**
 * 短信发送抽象。
 * 具体实现由 sms.provider（SMS_PROVIDER=log|aliyun|tencent）选择：
 * - log：仅开发/演示，验证码写日志，不真实发送；
 * - aliyun / tencent：直连对应云厂商 HTTP API，凭据全部来自环境变量。
 */
public interface SmsSender {

    /**
     * 发送验证码短信。失败时抛出 {@link SmsSendException}，由调用方转成可读错误。
     * 实现类不得把验证码、AccessKeySecret、SecretKey 写入日志。
     */
    void send(String phone, String code);

    /** 通道标识，用于日志与排障。 */
    String provider();
}
