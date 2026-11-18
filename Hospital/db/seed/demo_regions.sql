-- =============================================================
--  多地区演示数据（虚构机构，仅用于产品演示，不代表真实医院）
--  地区：成都市区 / 邛崃市 / 西安及周边（西安、咸阳、临潼）
--  特点：医生全部字段完整（含挂号费、职称、擅长、学历、履历、
--        简介、资质），并为每位医生生成未来 14 天可预约号源。
--  幂等：所有 ID 以 DEMO_ 开头，可重复执行；不会改动真实数据，
--        重复执行也不会重置已预约号源的 booked_count。
-- =============================================================
USE hospital_db;
SET NAMES utf8mb4;

-- ---------- 1. 演示医院 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_hospital;
CREATE TEMPORARY TABLE tmp_demo_hospital (
  code varchar(16) NOT NULL PRIMARY KEY,
  hospital_id varchar(64) NOT NULL,
  hospital_name varchar(128) NOT NULL,
  level varchar(16) NOT NULL,
  province varchar(32) NOT NULL,
  city varchar(32) NOT NULL,
  district varchar(32) NOT NULL,
  address varchar(128) NOT NULL,
  latitude double NOT NULL,
  longitude double NOT NULL,
  phone varchar(32) NOT NULL,
  flagship_dept varchar(255) NOT NULL,
  doctor_count int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_hospital VALUES
('CD1','DEMO_H_CD1','成都锦城医院','三级甲等','四川省','成都市','锦江区','成都市锦江区红星路三段88号（演示）',30.6460,104.0810,'028-88880001','呼吸内科,心血管内科,儿科',2),
('CD2','DEMO_H_CD2','成都天府医院','三级甲等','四川省','成都市','高新区','成都市高新区天府大道中段666号（演示）',30.5740,104.0660,'028-88880002','妇产科,儿科,消化内科',2),
('CD3','DEMO_H_CD3','成都青羊仁和医院','三级乙等','四川省','成都市','青羊区','成都市青羊区清江中路18号（演示）',30.6740,104.0550,'028-88880003','骨科,老年病科,内分泌科',2),
('QL1','DEMO_H_QL1','邛崃市临邛人民医院','二级甲等','四川省','成都市','邛崃市','邛崃市临邛街道文君街36号（演示）',30.4100,103.4620,'028-88880004','呼吸内科,消化内科,骨科',1),
('QL2','DEMO_H_QL2','邛崃市文君中医院','二级甲等','四川省','成都市','邛崃市','邛崃市文君街道南街12号（演示）',30.4200,103.4700,'028-88880005','神经内科,老年病科,内科',1),
('XA1','DEMO_H_XA1','西安雁南医院','三级甲等','陕西省','西安市','雁塔区','西安市雁塔区雁南四路18号（演示）',34.2180,108.9480,'029-88880006','心血管内科,神经内科,骨科',2),
('XA2','DEMO_H_XA2','西安长安医院','三级乙等','陕西省','西安市','长安区','西安市长安区韦曲街道西长安街66号（演示）',34.1550,108.9060,'029-88880007','呼吸内科,消化内科,儿科',2),
('XY1','DEMO_H_XY1','咸阳秦都医院','三级甲等','陕西省','咸阳市','秦都区','咸阳市秦都区人民西路38号（演示）',34.3300,108.7080,'029-88880008','心血管内科,内分泌科,肿瘤科',1),
('LT1','DEMO_H_LT1','西安临潼华清医院','二级甲等','陕西省','西安市','临潼区','西安市临潼区华清路12号（演示）',34.3720,109.2140,'029-88880009','骨科,皮肤科,眼科',1);

INSERT INTO hospital (hospital_id, hospital_name, address, phone, level, description, flagship_dept, website_url,
                      latitude, longitude, status, create_time, province, city, district)
SELECT h.hospital_id, h.hospital_name, h.address, h.phone, h.level,
       CONCAT('演示机构，数据为虚构示例，不代表真实医院。', h.hospital_name, '位于', h.province, h.city, h.district, '，用于分诊与预约功能演示。'),
       h.flagship_dept, '', h.latitude, h.longitude, 1, NOW(), h.province, h.city, h.district
FROM tmp_demo_hospital h
ON DUPLICATE KEY UPDATE
  hospital_name = VALUES(hospital_name), address = VALUES(address), phone = VALUES(phone),
  level = VALUES(level), description = VALUES(description), flagship_dept = VALUES(flagship_dept),
  latitude = VALUES(latitude), longitude = VALUES(longitude), status = 1,
  province = VALUES(province), city = VALUES(city), district = VALUES(district);

-- ---------- 2. 全科门诊科室 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dept;
CREATE TEMPORARY TABLE tmp_demo_dept (
  dept_no int NOT NULL PRIMARY KEY,
  dept_name varchar(32) NOT NULL,
  expertise varchar(255) NOT NULL,
  diseases text NOT NULL,
  service_time varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_dept VALUES
(1,'呼吸内科','慢性咳嗽、哮喘、慢阻肺、肺部感染诊治','咳嗽、发热、气喘、支气管炎、肺炎','08:00-12:00 14:00-17:30'),
(2,'消化内科','胃炎、反流性食管炎、腹泻、肝胆疾病诊治','胃痛、反酸、腹泻、呕吐、便秘、腹胀','08:00-12:00 14:00-17:30'),
(3,'心血管内科','高血压、冠心病、心律失常、心衰诊治','心慌、胸闷、高血压、心律不齐、水肿','08:00-12:00 14:00-17:30'),
(4,'神经内科','头痛、眩晕、失眠、脑血管病诊治','头痛、头晕、眩晕、失眠、手麻、面瘫','08:00-12:00 14:00-17:30'),
(5,'内分泌科','糖尿病、甲状腺疾病、肥胖、代谢综合征诊治','血糖、糖尿病、甲状腺、口渴、多尿','08:00-12:00 14:00-17:30'),
(6,'肾内科','肾炎、肾病综合征、尿路感染、血液透析','尿频、尿急、尿痛、血尿、泡沫尿','08:00-12:00 14:00-17:30'),
(7,'骨科','颈肩腰腿痛、关节疾病、骨折、运动损伤','腰痛、关节痛、骨折、扭伤、颈肩痛','08:00-12:00 14:00-17:30'),
(8,'外科','阑尾炎、疝气、甲状腺肿块、肛肠疾病','右下腹痛、阑尾、疝气、肿块、便血','08:00-12:00 14:00-17:30'),
(9,'妇产科','月经不调、妇科炎症、孕期保健、产检','月经、白带、怀孕、产检、下腹痛','08:00-12:00 14:00-17:30'),
(10,'儿科','儿童发热、呼吸道感染、腹泻、生长发育','儿童、宝宝、发热、咳嗽、腹泻','08:00-12:00 14:00-17:30'),
(11,'眼科','近视、白内障、青光眼、眼底病诊治','眼痛、眼红、视力下降、流泪、视物模糊','08:00-12:00 14:00-17:30'),
(12,'耳鼻喉科','鼻炎、鼻窦炎、中耳炎、咽喉疾病诊治','耳鸣、耳痛、听力下降、鼻塞、声音嘶哑','08:00-12:00 14:00-17:30'),
(13,'口腔科','龋齿、牙周病、智齿、口腔溃疡诊治','牙痛、牙龈出血、智齿、口腔溃疡','08:00-12:00 14:00-17:30'),
(14,'皮肤科','湿疹、皮炎、荨麻疹、痤疮、脱发诊治','皮疹、瘙痒、红斑、湿疹、水疱、脱发','08:00-12:00 14:00-17:30'),
(15,'肿瘤科','肿瘤筛查、化疗、放疗、靶向治疗','肿瘤、癌、化疗、放疗、结节','08:00-12:00 14:00-17:30'),
(16,'内科','常见内科疾病、慢病管理、健康咨询','乏力、低热、浑身没劲、没力气','08:00-12:00 14:00-17:30'),
(17,'老年病科','老年慢病、多病共存、老年综合评估','老年、慢病、乏力、头晕、食欲下降','08:00-12:00 14:00-17:30');

INSERT INTO department (dept_id, hospital_id, dept_name, description, diseases, service_time, is_flagship, flagship_level, create_time, is_deleted)
SELECT CONCAT('DEMO_DEPT_', h.code, '_', LPAD(d.dept_no, 2, '0')),
       h.hospital_id, d.dept_name,
       CONCAT(d.dept_name, '门诊（演示数据）'), d.diseases, d.service_time,
       CASE WHEN (
         (h.code IN ('CD1','XA1') AND d.dept_no IN (1,3)) OR
         (h.code = 'CD2' AND d.dept_no IN (9,10)) OR
         (h.code = 'CD3' AND d.dept_no IN (7,17)) OR
         (h.code IN ('QL1','QL2') AND d.dept_no IN (1,2,7)) OR
         (h.code = 'XA2' AND d.dept_no IN (1,2,10)) OR
         (h.code = 'XY1' AND d.dept_no IN (3,5,15)) OR
         (h.code = 'LT1' AND d.dept_no IN (7,14,11))
       ) THEN 1 ELSE 0 END,
       CASE
         WHEN h.code IN ('CD1','XA1') AND d.dept_no IN (1,3) THEN 2
         WHEN h.code = 'CD2' AND d.dept_no IN (9,10) THEN 1
         WHEN h.code = 'CD3' AND d.dept_no IN (7,17) THEN 3
         WHEN h.code IN ('QL1','QL2') AND d.dept_no IN (1,2,7) THEN 3
         WHEN h.code = 'XA2' AND d.dept_no IN (1,2,10) THEN 3
         WHEN h.code = 'XY1' AND d.dept_no IN (3,5,15) THEN 1
         WHEN h.code = 'LT1' AND d.dept_no IN (7,14,11) THEN 2
         ELSE 0
       END,
       NOW(), 0
FROM tmp_demo_hospital h
CROSS JOIN tmp_demo_dept d
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
  dept_name = VALUES(dept_name), description = VALUES(description), diseases = VALUES(diseases),
  service_time = VALUES(service_time), is_flagship = VALUES(is_flagship),
  flagship_level = VALUES(flagship_level), is_deleted = 0;

-- ---------- 3. 医生（每位医生都带完整资料） ----------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_seq;
CREATE TEMPORARY TABLE tmp_demo_seq (n int NOT NULL PRIMARY KEY);
INSERT INTO tmp_demo_seq VALUES (1),(2);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_name;
CREATE TEMPORARY TABLE tmp_demo_name (
  no int NOT NULL PRIMARY KEY,
  name varchar(32) NOT NULL,
  gender varchar(8) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_name VALUES
(1,'张伟','男'),(2,'王强','男'),(3,'李军','男'),(4,'刘洋','男'),(5,'陈杰','男'),(6,'杨磊','男'),
(7,'赵鹏','男'),(8,'黄涛','男'),(9,'周斌','男'),(10,'吴昊','男'),(11,'徐峰','男'),(12,'孙浩','男'),
(13,'马超','男'),(14,'朱明','男'),(15,'胡健','男'),(16,'郭亮','男'),(17,'何勇','男'),(18,'高翔','男'),
(19,'林涛','男'),(20,'罗刚','男'),(21,'郑凯','男'),(22,'梁晨','男'),(23,'宋博','男'),(24,'唐锐','男'),
(25,'王芳','女'),(26,'李娜','女'),(27,'张敏','女'),(28,'刘静','女'),(29,'陈燕','女'),(30,'杨丽','女'),
(31,'赵娟','女'),(32,'黄婷','女'),(33,'周莉','女'),(34,'吴倩','女'),(35,'徐颖','女'),(36,'孙悦','女'),
(37,'马琳','女'),(38,'朱丹','女'),(39,'胡雪','女'),(40,'郭静','女'),(41,'何雯','女'),(42,'高雅','女'),
(43,'林琳','女'),(44,'罗曼','女'),(45,'郑洁','女'),(46,'梁静','女'),(47,'宋佳','女'),(48,'唐宁','女');

INSERT INTO doctor (doctor_id, hospital_id, dept_id, name, gender, title, expertise, phone,
                    education, experience, moral, status, consultation_fee, username, password)
SELECT CONCAT('DEMO_D_', h.code, '_', LPAD(d.dept_no, 2, '0'), '_', s.n),
       h.hospital_id,
       CONCAT('DEMO_DEPT_', h.code, '_', LPAD(d.dept_no, 2, '0')),
       nm.name, nm.gender,
       CASE WHEN h.doctor_count = 1 THEN '副主任医师'
            WHEN s.n = 1 THEN '主任医师'
            ELSE '主治医师' END,
       d.expertise,
       CONCAT('199', LPAD(MOD(CRC32(CONCAT(h.code, d.dept_no, 'phone', s.n)), 100000000), 8, '0')),
       CASE WHEN h.city = '成都市' AND h.district = '邛崃市' THEN '医学本科，毕业于成都医学院'
            WHEN h.city = '成都市' THEN '医学硕士，毕业于四川大学华西临床医学院'
            ELSE '医学博士，毕业于西安交通大学医学院' END,
       CONCAT('从事', d.dept_name, '临床工作',
              CASE WHEN h.doctor_count = 1 THEN '15' WHEN s.n = 1 THEN '20' ELSE '8' END,
              '年，累计接诊超过',
              CASE WHEN h.doctor_count = 1 THEN '1.5万' WHEN s.n = 1 THEN '2万' ELSE '1万' END,
              '人次'),
       '医德考评优秀，无不良执业记录（演示数据）',
       1,
       CASE WHEN h.doctor_count = 1 THEN 40 WHEN s.n = 1 THEN 50 ELSE 25 END,
       CONCAT('demo_', LOWER(h.code), '_', LPAD(d.dept_no, 2, '0'), '_', s.n),
       'Demo@123456'
FROM tmp_demo_hospital h
CROSS JOIN tmp_demo_dept d
CROSS JOIN tmp_demo_seq s
JOIN tmp_demo_name nm ON nm.no = MOD(CRC32(CONCAT(h.code, d.dept_no, s.n)), 48) + 1
WHERE s.n <= h.doctor_count
ON DUPLICATE KEY UPDATE
  name = VALUES(name), gender = VALUES(gender), title = VALUES(title), expertise = VALUES(expertise),
  phone = VALUES(phone), education = VALUES(education), experience = VALUES(experience),
  moral = VALUES(moral), status = 1, consultation_fee = VALUES(consultation_fee),
  username = VALUES(username), password = VALUES(password);

INSERT INTO doctor_detail (detail_id, doctor_id, consultation_fee, education, expertise_desc,
                           introduction, work_experience, awards, qualification)
SELECT CONCAT('DEMO_DD_', d.doctor_id), d.doctor_id, d.consultation_fee, d.education, d.expertise,
       CONCAT('演示医生：', d.name, '，', h.hospital_name, ' ', dep.dept_name, ' ', d.title,
              '。本条目为系统演示数据，非真实执业信息。'),
       d.experience,
       CASE WHEN d.title = '主任医师' THEN '省级医学科技进步二等奖（演示数据）'
            WHEN d.title = '副主任医师' THEN '市级优秀医师（演示数据）'
            ELSE '院级优秀青年医师（演示数据）' END,
       CONCAT(d.title, '资格证书（演示数据）')
FROM doctor d
JOIN tmp_demo_hospital h ON h.hospital_id = d.hospital_id
JOIN department dep ON dep.dept_id = d.dept_id
WHERE d.doctor_id LIKE 'DEMO_D_%'
ON DUPLICATE KEY UPDATE
  consultation_fee = VALUES(consultation_fee), education = VALUES(education),
  expertise_desc = VALUES(expertise_desc), introduction = VALUES(introduction),
  work_experience = VALUES(work_experience), awards = VALUES(awards),
  qualification = VALUES(qualification);

-- ---------- 4. 医生出诊设置 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dow;
CREATE TEMPORARY TABLE tmp_demo_dow (dow int NOT NULL PRIMARY KEY);
INSERT INTO tmp_demo_dow VALUES (1),(2),(3),(4),(5),(6),(7);

INSERT INTO doctor_schedule_setting (setting_id, doctor_id, day_of_week, enabled, start_time, end_time,
                                     slot_duration, max_patients_per_slot)
SELECT CONCAT('DEMO_DSS_', d.doctor_id, '_', w.dow), d.doctor_id, w.dow, 1,
       '08:00:00', '17:30:00', 20, 3
FROM doctor d
CROSS JOIN tmp_demo_dow w
WHERE d.doctor_id LIKE 'DEMO_D_%'
ON DUPLICATE KEY UPDATE
  enabled = 1, start_time = VALUES(start_time), end_time = VALUES(end_time),
  slot_duration = VALUES(slot_duration), max_patients_per_slot = VALUES(max_patients_per_slot);

-- ---------- 5. 未来 14 天号源 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_day;
CREATE TEMPORARY TABLE tmp_demo_day (day_offset int NOT NULL PRIMARY KEY);
INSERT INTO tmp_demo_day VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_slot;
CREATE TEMPORARY TABLE tmp_demo_slot (slot_no int NOT NULL PRIMARY KEY, time_slot varchar(16) NOT NULL);
INSERT INTO tmp_demo_slot VALUES
(1,'08:00-08:20'),(2,'09:00-09:20'),(3,'10:00-10:20'),
(4,'14:00-14:20'),(5,'15:00-15:20'),(6,'16:00-16:20');

INSERT INTO schedule (schedule_id, doctor_id, hospital_id, work_date, time_slot,
                      max_patients, booked_count, status, version)
SELECT CONCAT('DEMO_S_', d.doctor_id, '_', day.day_offset, '_', slot.slot_no),
       d.doctor_id, d.hospital_id,
       DATE_ADD(CURDATE(), INTERVAL day.day_offset DAY),
       slot.time_slot, 3, 0, 1, 0
FROM doctor d
CROSS JOIN tmp_demo_day day
CROSS JOIN tmp_demo_slot slot
WHERE d.doctor_id LIKE 'DEMO_D_%'
ON DUPLICATE KEY UPDATE
  work_date = VALUES(work_date), time_slot = VALUES(time_slot),
  max_patients = VALUES(max_patients), status = VALUES(status);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_hospital;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dept;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_seq;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_name;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dow;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_day;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_slot;

SELECT '演示数据导入完成' AS message,
       (SELECT COUNT(*) FROM hospital WHERE hospital_id LIKE 'DEMO_H_%') AS demo_hospitals,
       (SELECT COUNT(*) FROM department WHERE dept_id LIKE 'DEMO_DEPT_%') AS demo_departments,
       (SELECT COUNT(*) FROM doctor WHERE doctor_id LIKE 'DEMO_D_%') AS demo_doctors,
       (SELECT COUNT(*) FROM doctor_detail WHERE doctor_id LIKE 'DEMO_D_%') AS demo_doctor_details,
       (SELECT COUNT(*) FROM schedule WHERE schedule_id LIKE 'DEMO_S_%' AND work_date >= CURDATE()) AS demo_future_schedules;
