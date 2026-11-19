-- Direction A 视觉稿字段补齐（可选列 + 搜索普通索引）
-- 原则：能由现有表计算的字段不落列；这里只放外部来源字段，缺省走 demo mock。
-- 执行一次即可；脚本不修改任何现有数据。

SET NAMES utf8mb4;

-- 医院：性质/院区/徽标/门诊时间/导医台/急诊电话
ALTER TABLE hospital
    ADD COLUMN nature VARCHAR(32) NULL COMMENT '医院性质：公立综合/公立专科/民营综合',
    ADD COLUMN campus VARCHAR(64) NULL COMMENT '院区名称',
    ADD COLUMN logo_url VARCHAR(255) NULL COMMENT '院徽地址',
    ADD COLUMN outpatient_hours VARCHAR(64) NULL COMMENT '门诊时间，如 08:00-17:30',
    ADD COLUMN guide_desk_phone VARCHAR(32) NULL COMMENT '导医台电话',
    ADD COLUMN emergency_phone VARCHAR(32) NULL COMMENT '急诊电话';

-- 科室：分组与楼层位置
ALTER TABLE department
    ADD COLUMN dept_group VARCHAR(16) NULL COMMENT '内科/外科/专科/医技',
    ADD COLUMN location VARCHAR(128) NULL COMMENT '门诊楼层位置';

-- 医生：从业年限/头像/资质标签
ALTER TABLE doctor
    ADD COLUMN years INT NULL COMMENT '从业年限',
    ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT '头像地址',
    ADD COLUMN tags VARCHAR(255) NULL COMMENT '资质标签，逗号分隔';

-- 预约：排队序号/医保金额/支付状态/报到时间
ALTER TABLE appointment
    ADD COLUMN queue_no INT NULL COMMENT '候诊排队序号',
    ADD COLUMN insurance_amount DECIMAL(10, 2) NULL COMMENT '医保报销金额',
    ADD COLUMN pay_status VARCHAR(16) NULL COMMENT 'PAID/UNPAID',
    ADD COLUMN checkin_time DATETIME NULL COMMENT '报到时间';

-- 搜索普通索引（不做全文索引）
CREATE INDEX idx_hospital_name ON hospital (hospital_name);
CREATE INDEX idx_department_name ON department (dept_name);
CREATE INDEX idx_doctor_name ON doctor (name);
CREATE INDEX idx_article_title ON hospital_article (title);
