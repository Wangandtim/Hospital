-- =============================================================
-- V6：医生照片与荣誉证书
-- 1) doctor_detail 增加照片地址（可空）
-- 2) 新建医生荣誉证书表（一位医生多张）
-- =============================================================

ALTER TABLE doctor_detail
    ADD COLUMN photo_url VARCHAR(500) NULL COMMENT '医生照片 URL';

CREATE TABLE doctor_certificate (
    certificate_id VARCHAR(32) NOT NULL COMMENT '证书主键',
    doctor_id      VARCHAR(20) NOT NULL COMMENT '所属医生',
    title          VARCHAR(128) NOT NULL COMMENT '证书名称',
    issuer         VARCHAR(128) NULL COMMENT '颁发机构',
    issued_date    DATE NULL COMMENT '颁发日期',
    image_url      VARCHAR(500) NULL COMMENT '证书图片 URL',
    sort_order     INT NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    create_time    DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (certificate_id),
    KEY idx_doctor_certificate_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生荣誉证书';
