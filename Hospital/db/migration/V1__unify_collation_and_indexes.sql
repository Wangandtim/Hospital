-- =============================================================
-- V1__unify_collation_and_indexes.sql
-- 医院预约系统 数据库层统一迁移
-- 目标：库/表/列 字符集统一 utf8mb4 + utf8mb4_unicode_ci；
--       归档并下线无实体映射的遗留表；补齐业务查询索引。
-- 幂等：可重复执行。
-- 回滚：db/rollback/R1__unify_collation_and_indexes.sql
-- =============================================================
SET NAMES utf8mb4;

-- ---------- 1. 库级规范 ----------
ALTER DATABASE `hospital_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------- 2. 归档库 ----------
CREATE DATABASE IF NOT EXISTS `hospital_db_archive`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- ---------- 3. 归档遗留表后下线 ----------
DROP PROCEDURE IF EXISTS `_arch_table`;
DELIMITER $$
CREATE PROCEDURE `_arch_table`(IN src VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables
             WHERE table_schema = 'hospital_db' AND table_name = src) THEN
    SET @s = CONCAT('DROP TABLE IF EXISTS `hospital_db_archive`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

    SET @s = CONCAT('CREATE TABLE `hospital_db_archive`.`', src, '` LIKE `hospital_db`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

    SET @s = CONCAT('ALTER TABLE `hospital_db_archive`.`', src,
                    '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

    SET @s = CONCAT('INSERT INTO `hospital_db_archive`.`', src,
                    '` SELECT * FROM `hospital_db`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

    SET @s = CONCAT('DROP TABLE `hospital_db`.`', src, '`');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
  END IF;
END$$
DELIMITER ;

-- 注意顺序：registration_attachment 通过外键引用 user，必须先归档子表
CALL `_arch_table`('registration_attachment');
CALL `_arch_table`('user');
CALL `_arch_table`('emergency_login_log');
DROP PROCEDURE IF EXISTS `_arch_table`;

-- ---------- 4. 业务索引 ----------
DROP PROCEDURE IF EXISTS `_add_index`;
DELIMITER $$
CREATE PROCEDURE `_add_index`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(255))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables
             WHERE table_schema = 'hospital_db' AND table_name = tbl)
     AND NOT EXISTS (SELECT 1 FROM information_schema.statistics
                     WHERE table_schema = 'hospital_db' AND table_name = tbl AND index_name = idx) THEN
    SET @s = CONCAT('ALTER TABLE `hospital_db`.`', tbl, '` ADD INDEX `', idx, '` (', cols, ')');
    PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
  END IF;
END$$
DELIMITER ;

-- 排班：按医生+日期、按医院+日期（最高频查询）
CALL `_add_index`('schedule', 'idx_schedule_doctor_date',   '`doctor_id`, `work_date`');
CALL `_add_index`('schedule', 'idx_schedule_hospital_date', '`hospital_id`, `work_date`');

-- 预约：患者列表、微信用户列表、医生日程、排班回查
CALL `_add_index`('appointment', 'idx_appointment_patient',       '`patient_id`, `create_time`');
CALL `_add_index`('appointment', 'idx_appointment_openid',        '`openid`, `create_time`');
CALL `_add_index`('appointment', 'idx_appointment_doctor_date',   '`doctor_id`, `appointment_date`');
CALL `_add_index`('appointment', 'idx_appointment_schedule',      '`schedule_id`');

-- 医生：按医院、按科室、按登录名
CALL `_add_index`('doctor', 'idx_doctor_hospital', '`hospital_id`');
CALL `_add_index`('doctor', 'idx_doctor_dept',     '`dept_id`');
CALL `_add_index`('doctor', 'idx_doctor_username', '`username`');

-- 科室：按医院
CALL `_add_index`('department', 'idx_department_hospital', '`hospital_id`');

-- 医院：地区筛选、状态筛选
CALL `_add_index`('hospital', 'idx_hospital_region', '`province`, `city`, `district`');
CALL `_add_index`('hospital', 'idx_hospital_status', '`status`');

-- 患者：手机号登录
CALL `_add_index`('patient', 'idx_patient_phone', '`phone`');

-- 医生排班模板：按医生
CALL `_add_index`('doctor_schedule_setting', 'idx_dss_doctor', '`doctor_id`');

-- 急诊登录日志：按医院+时间
CALL `_add_index`('emergency_log', 'idx_emergency_log_hospital_time', '`hospital_id`, `login_time`');

DROP PROCEDURE IF EXISTS `_add_index`;
