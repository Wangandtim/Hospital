
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `admin_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `hospital_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `real_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `api_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_key_expire` datetime(6) DEFAULT NULL,
  `audit_comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audit_status` int DEFAULT NULL,
  `audit_time` datetime(6) DEFAULT NULL,
  `id_card_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `work_card_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `UKgfn44sntic2k93auag97juyij` (`username`),
  KEY `fk_admin_hospital` (`hospital_id`),
  CONSTRAINT `fk_admin_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appointment` (
  `appointment_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `appointment_date` date NOT NULL,
  `cancel_time` datetime(6) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `doctor_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `patient_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `schedule_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` int DEFAULT NULL,
  `time_slot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `openid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `questionnaire_data` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`appointment_id`),
  KEY `idx_appointment_patient` (`patient_id`,`create_time`),
  KEY `idx_appointment_openid` (`openid`,`create_time`),
  KEY `idx_appointment_doctor_date` (`doctor_id`,`appointment_date`),
  KEY `idx_appointment_schedule` (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `dept_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `dept_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_time` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `diseases` text COLLATE utf8mb4_unicode_ci,
  `is_flagship` int DEFAULT NULL,
  PRIMARY KEY (`dept_id`),
  KEY `idx_department_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor` (
  `doctor_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `consultation_fee` int DEFAULT NULL,
  `dept_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expertise` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `education` text COLLATE utf8mb4_unicode_ci,
  `experience` text COLLATE utf8mb4_unicode_ci,
  `moral` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`doctor_id`),
  KEY `idx_doctor_hospital` (`hospital_id`),
  KEY `idx_doctor_dept` (`dept_id`),
  KEY `idx_doctor_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_detail` (
  `detail_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `awards` text COLLATE utf8mb4_unicode_ci,
  `consultation_fee` int DEFAULT NULL,
  `doctor_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `education` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expertise_desc` text COLLATE utf8mb4_unicode_ci,
  `introduction` text COLLATE utf8mb4_unicode_ci,
  `qualification` text COLLATE utf8mb4_unicode_ci,
  `work_experience` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `UKkw10ii4cxmg8km7k2s1ktcjaq` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_schedule_setting` (
  `setting_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `day_of_week` int NOT NULL,
  `doctor_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` int DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `max_patients_per_slot` int DEFAULT NULL,
  `slot_duration` int DEFAULT NULL,
  `start_time` time(6) DEFAULT NULL,
  PRIMARY KEY (`setting_id`),
  KEY `idx_dss_doctor` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emergency_log` (
  `log_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hospital_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `login_time` datetime(6) NOT NULL,
  `logout_time` datetime(6) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `token_expire_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_emergency_log_hospital_time` (`hospital_id`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital` (
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `hospital_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `latitude` double DEFAULT NULL,
  `level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT NULL,
  `flagship_dept` text COLLATE utf8mb4_unicode_ci,
  `website_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`hospital_id`),
  KEY `idx_hospital_region` (`province`,`city`,`district`),
  KEY `idx_hospital_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_key` (
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `daily_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expire_time` datetime(6) NOT NULL,
  `last_renew_time` datetime(6) DEFAULT NULL,
  `status` int DEFAULT NULL,
  PRIMARY KEY (`hospital_id`),
  UNIQUE KEY `UK62ubv5i52erhwds0cuf1rf6ij` (`daily_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient` (
  `patient_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `age` int DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `medical_history` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  KEY `idx_patient_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule` (
  `schedule_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `booked_count` int DEFAULT NULL,
  `doctor_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_patients` int DEFAULT NULL,
  `status` int DEFAULT NULL,
  `time_slot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int DEFAULT NULL,
  `work_date` date NOT NULL,
  `hospital_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`schedule_id`),
  KEY `idx_schedule_doctor_date` (`doctor_id`,`work_date`),
  KEY `idx_schedule_hospital_date` (`hospital_id`,`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wechat_user` (
  `openid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `last_latitude` double DEFAULT NULL,
  `last_location_time` datetime(6) DEFAULT NULL,
  `last_longitude` double DEFAULT NULL,
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unionid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patient_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`openid`),
  UNIQUE KEY `UKfyiggrt2639a6jm6f5y81mw1s` (`patient_id`),
  CONSTRAINT `FKcd2rl6wvwtcn5aaaslw95va6g` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
CREATE TABLE IF NOT EXISTS audit_log (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(64) DEFAULT NULL,
  `target_id` varchar(64) DEFAULT NULL,
  `operator_role` varchar(32) DEFAULT NULL,
  `operator_id` varchar(64) DEFAULT NULL,
  `detail` varchar(1000) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctor_duty_rule (
  `rule_id` varchar(64) NOT NULL,
  `hospital_id` varchar(64) NOT NULL,
  `doctor_id` varchar(64) NOT NULL,
  `duty_date` date NOT NULL,
  `start_time` varchar(8) NOT NULL,
  `end_time` varchar(8) NOT NULL,
  `status` int NOT NULL DEFAULT '1',
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_duty_doctor_date` (`doctor_id`,`duty_date`),
  KEY `idx_duty_hospital_date` (`hospital_id`,`duty_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS emergency_request (
  `id` varchar(40) NOT NULL,
  `hospital_id` varchar(20) DEFAULT NULL,
  `doctor_id` varchar(40) DEFAULT NULL,
  `doctor_name` varchar(80) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` int DEFAULT '0',
  `approver_id` varchar(40) DEFAULT NULL,
  `approver_name` varchar(80) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `token` varchar(80) DEFAULT NULL,
  `token_expire_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `doctor_title` varchar(50) DEFAULT NULL,
  `cert_id_path` varchar(255) DEFAULT NULL,
  `cert_license_path` varchar(255) DEFAULT NULL,
  `cert_verified` int NOT NULL DEFAULT '0',
  `cert_verified_by` varchar(40) DEFAULT NULL,
  `cert_verified_time` datetime DEFAULT NULL,
  `cert_verify_remark` varchar(255) DEFAULT NULL,
  `auto_grant` int NOT NULL DEFAULT '1',
  `report_scope` text,
  `report_impact` text,
  `report_complete` int NOT NULL DEFAULT '0',
  `report_time` datetime DEFAULT NULL,
  `report_deadline` datetime DEFAULT NULL,
  `confirm_deadline` datetime DEFAULT NULL,
  `confirm_by` varchar(40) DEFAULT NULL,
  `confirm_by_name` varchar(80) DEFAULT NULL,
  `confirm_time` datetime DEFAULT NULL,
  `confirm_remark` varchar(255) DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `end_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_emergency_request` (`hospital_id`,`status`,`create_time`),
  KEY `idx_er_status_expire` (`status`,`token_expire_time`),
  KEY `idx_er_status_report` (`status`,`report_deadline`),
  KEY `idx_er_status_confirm` (`status`,`confirm_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS emergency_operation (
  `id` varchar(40) NOT NULL,
  `request_id` varchar(40) DEFAULT NULL,
  `hospital_id` varchar(20) DEFAULT NULL,
  `doctor_id` varchar(40) DEFAULT NULL,
  `token` varchar(80) DEFAULT NULL,
  `method` varchar(10) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `target_type` varchar(20) DEFAULT NULL,
  `target_id` varchar(40) DEFAULT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_eo_request` (`request_id`,`create_time`),
  KEY `idx_eo_token` (`token`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS emergency_snapshot (
  `id` varchar(40) NOT NULL,
  `hospital_id` varchar(20) DEFAULT NULL,
  `table_name` varchar(40) DEFAULT NULL,
  `record_id` varchar(64) DEFAULT NULL,
  `data_json` longtext,
  `token` varchar(80) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_snapshot_hospital` (`hospital_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hospital_article (
  `article_id` varchar(40) NOT NULL,
  `hospital_id` varchar(40) NOT NULL,
  `title` varchar(200) NOT NULL,
  `summary` varchar(300) DEFAULT NULL,
  `content` text,
  `cover_url` varchar(500) DEFAULT NULL,
  `category` varchar(20) DEFAULT '文章',
  `status` int DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`article_id`),
  KEY `idx_article_hospital` (`hospital_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patient_feedback (
  `feedback_id` varchar(32) NOT NULL,
  `patient_id` varchar(32) DEFAULT NULL,
  `hospital_id` varchar(32) DEFAULT NULL,
  `hospital_name` varchar(100) DEFAULT NULL,
  `appointment_id` varchar(32) DEFAULT NULL,
  `rating` tinyint DEFAULT NULL,
  `tags` varchar(200) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `contact` varchar(50) DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`feedback_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS schedule_weekly_confirm (
  `confirm_id` varchar(64) NOT NULL,
  `doctor_id` varchar(64) NOT NULL,
  `week_start` date NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `remind_time` datetime DEFAULT NULL,
  `confirm_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`confirm_id`),
  UNIQUE KEY `uk_doctor_week` (`doctor_id`,`week_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_record (
  `payment_id` varchar(32) NOT NULL,
  `appointment_id` varchar(255) NOT NULL,
  `patient_id` varchar(255) DEFAULT NULL,
  `amount` double DEFAULT NULL,
  `payment_method` varchar(20) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `order_no` varchar(64) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `idx_payment_appointment` (`appointment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS triage_session (
  triage_id VARCHAR(36) NOT NULL COMMENT '会话ID',
  patient_id VARCHAR(36) NOT NULL COMMENT '令牌归属患者',
  subject VARCHAR(12) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/ELDER',
  mode VARCHAR(12) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/ELDER',
  raw_input TEXT COMMENT '原始症状',
  normalized_input VARCHAR(512) COMMENT '归一化文本',
  structured_symptoms TEXT COMMENT '结构化症状JSON',
  red_flag TINYINT NOT NULL DEFAULT 0,
  red_flag_pattern VARCHAR(64) COMMENT '命中红旗规则',
  red_flag_reason VARCHAR(255) COMMENT '红旗原因',
  age INT COMMENT '本次分诊年龄',
  age_band VARCHAR(12) COMMENT 'CHILD/YOUNG/MIDDLE/ELDER',
  gender VARCHAR(8) COMMENT 'MALE/FEMALE',
  pregnant VARCHAR(12) COMMENT 'YES/NO/UNKNOWN',
  medical_history VARCHAR(512) COMMENT '既往史快照',
  dept_name VARCHAR(64) COMMENT '推荐科室',
  confidence DECIMAL(4,3) COMMENT '置信度0-1',
  confidence_tier VARCHAR(12) COMMENT 'HIGH/MEDIUM/LOW/RED_FLAG',
  need_expert VARCHAR(16) COMMENT 'REQUIRED/RECOMMENDED/NOT_NEEDED',
  expert_reason VARCHAR(255) COMMENT '专家判定依据',
  hospital_id VARCHAR(36),
  doctor_id VARCHAR(36),
  schedule_id VARCHAR(36),
  match_score INT,
  match_reason VARCHAR(512),
  alternatives TEXT COMMENT '降级备选JSON',
  evidence TEXT COMMENT '规则命中依据JSON',
  status VARCHAR(16) NOT NULL DEFAULT 'ASSESSED' COMMENT 'NEED_PROFILE/ASSESSED/CONFIRMED/REJECTED/LOCKED/BOOKED/FAILED',
  rule_version VARCHAR(32),
  create_time DATETIME NOT NULL,
  update_time DATETIME,
  confirm_time DATETIME,
  book_time DATETIME,
  PRIMARY KEY (triage_id),
  KEY idx_triage_session_patient (patient_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分诊会话';

CREATE TABLE IF NOT EXISTS triage_lock (
  lock_id VARCHAR(36) NOT NULL,
  triage_id VARCHAR(36) NOT NULL,
  patient_id VARCHAR(36),
  schedule_id VARCHAR(36),
  doctor_id VARCHAR(36),
  hospital_id VARCHAR(36),
  dept_id VARCHAR(36),
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1锁定 2已出单 0已释放/过期',
  expire_at DATETIME NOT NULL,
  release_reason VARCHAR(64),
  appointment_id VARCHAR(36),
  create_time DATETIME NOT NULL,
  release_time DATETIME,
  PRIMARY KEY (lock_id),
  KEY idx_triage_lock_triage (triage_id, status),
  KEY idx_triage_lock_patient (patient_id, status),
  KEY idx_triage_lock_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分诊号源锁';

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

