package com.hospital.exception;

/**
 * 内容不存在：用于“关闭公告/未发布”等不应该对患者暴露的场景，统一映射为 404。
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
