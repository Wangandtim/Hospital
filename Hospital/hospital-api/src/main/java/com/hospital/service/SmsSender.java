package com.hospital.service;

/**
 * 短信发送抽象。
 * 当前默认实现只把验证码打到服务端日志，便于本地联调；
 * 接入真实短信平台时新增一个实现类替换即可，无需改动业务层。
 */
public interface SmsSender {

    /**
     * 发送验证码短信。
     *
     * @return 是否发送成功
     */
    boolean send(String phone, String code);
}
