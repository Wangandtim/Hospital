-- 医生出诊规则表（生产库 ddl-auto = none，需手工执行一次）
-- 规则按「具体日期 + 起止时段」设定，用于医院管理员核对医生是否在岗、本周时段是否达标
CREATE TABLE IF NOT EXISTS doctor_duty_rule (
  rule_id VARCHAR(64) PRIMARY KEY,
  hospital_id VARCHAR(64) NOT NULL,
  doctor_id VARCHAR(64) NOT NULL,
  duty_date DATE NOT NULL,
  start_time VARCHAR(8) NOT NULL,
  end_time VARCHAR(8) NOT NULL,
  status INT NOT NULL DEFAULT 1,
  created_by VARCHAR(64),
  created_at DATETIME NOT NULL,
  INDEX idx_duty_doctor_date (doctor_id, duty_date),
  INDEX idx_duty_hospital_date (hospital_id, duty_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
