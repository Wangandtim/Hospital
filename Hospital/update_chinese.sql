-- 更新医院名称为中文
UPDATE hospital SET hospital_name = '邛崃市医疗中心医院', address = '四川省邛崃市文君街' WHERE hospital_id = 'H001';
UPDATE hospital SET hospital_name = '西安交通大学第一附属医院', address = '西安市雁塔区雁塔西路' WHERE hospital_id = 'H002';
UPDATE hospital SET hospital_name = '西京医院', address = '西安市新城区长乐西路' WHERE hospital_id = 'H003';
UPDATE hospital SET hospital_name = '陕西省人民医院', address = '西安市碑林区友谊西路' WHERE hospital_id = 'H004';
UPDATE hospital SET hospital_name = '四川大学华西医院', address = '成都市武侯区国学巷' WHERE hospital_id = 'H005';
UPDATE hospital SET hospital_name = '西安医学院第二附属医院', address = '西安市新城区尚勤路' WHERE hospital_id = 'H006';
UPDATE hospital SET hospital_name = '西安市中心医院', address = '西安市新城区西五路' WHERE hospital_id = 'H007';
UPDATE hospital SET hospital_name = '西安市第一医院', address = '西安市碑林区南大街' WHERE hospital_id = 'H008';
UPDATE hospital SET hospital_name = '西安市第三医院', address = '西安市未央区凤城三路东段' WHERE hospital_id = 'H009';
UPDATE hospital SET hospital_name = '西北妇女儿童医院', address = '西安市雁塔区雁翔路' WHERE hospital_id = 'H010';

-- 更新科室名称为中文
UPDATE department SET name = '内科', description = '呼吸、消化、心血管疾病', service_time = '周一至周日 08:00-17:00' WHERE id = 'DEPT001';
UPDATE department SET name = '外科', description = '普通外科、骨科、神经外科', service_time = '周一至周日 08:00-17:00' WHERE id = 'DEPT002';
UPDATE department SET name = '儿科', description = '儿童疾病、预防接种', service_time = '周一至周日 08:00-18:00' WHERE id = 'DEPT003';
UPDATE department SET name = '妇产科', description = '妇科、产前检查、分娩', service_time = '周一至周日 08:00-17:00' WHERE id = 'DEPT004';
UPDATE department SET name = '急诊科', description = '24小时急诊服务', service_time = '24小时' WHERE id = 'DEPT005';
UPDATE department SET name = '眼科', description = '眼科疾病、视力检查', service_time = '周一至周六 08:00-17:00' WHERE id = 'DEPT006';
UPDATE department SET name = '口腔科', description = '口腔护理、种植牙、正畸', service_time = '周一至周六 08:00-17:00' WHERE id = 'DEPT007';
UPDATE department SET name = '皮肤科', description = '皮肤病、性病、皮肤科', service_time = '周一至周六 08:00-17:00' WHERE id = 'DEPT008';

-- 更新医生姓名和职称为中文
UPDATE doctor SET name = '张建国', title = '主任医师', expertise = '心血管疾病、高血压' WHERE doctor_id = 'DOC001';
UPDATE doctor SET name = '李明华', title = '副主任医师', expertise = '消化系统疾病、胃病' WHERE doctor_id = 'DOC002';
UPDATE doctor SET name = '王芳', title = '主治医师', expertise = '呼吸系统疾病、肺炎' WHERE doctor_id = 'DOC003';
UPDATE doctor SET name = '赵伟', title = '主任医师', expertise = '普通外科、胃肠外科' WHERE doctor_id = 'DOC004';
UPDATE doctor SET name = '陈静', title = '副主任医师', expertise = '骨科、关节置换' WHERE doctor_id = 'DOC005';
UPDATE doctor SET name = '刘洋', title = '主治医师', expertise = '创伤、急诊外科' WHERE doctor_id = 'DOC006';
UPDATE doctor SET name = '周小红', title = '主任医师', expertise = '小儿呼吸、新生儿科' WHERE doctor_id = 'DOC007';
UPDATE doctor SET name = '吴丽娟', title = '副主任医师', expertise = '儿童保健、预防接种' WHERE doctor_id = 'DOC008';
UPDATE doctor SET name = '孙梅', title = '主任医师', expertise = '妇科肿瘤、宫腔镜' WHERE doctor_id = 'DOC009';
UPDATE doctor SET name = '郑颖', title = '副主任医师', expertise = '产前检查、高危妊娠' WHERE doctor_id = 'DOC010';
UPDATE doctor SET name = '钱进', title = '主任医师', expertise = '急诊医学、重症监护' WHERE doctor_id = 'DOC011';
UPDATE doctor SET name = '马小东', title = '副主任医师', expertise = '急性中毒、创伤' WHERE doctor_id = 'DOC012';
UPDATE doctor SET name = '徐光明', title = '主任医师', expertise = '白内障、青光眼' WHERE doctor_id = 'DOC013';
UPDATE doctor SET name = '郭婷婷', title = '副主任医师', expertise = '种植牙、正畸' WHERE doctor_id = 'DOC014';
UPDATE doctor SET name = '韩雪', title = '主治医师', expertise = '过敏性皮肤病、湿疹' WHERE doctor_id = 'DOC015';

-- 更新管理员姓名为中文
UPDATE admin SET real_name = '超级管理员' WHERE admin_id = 'S001';
UPDATE admin SET real_name = '邛崃医院管理员' WHERE admin_id = 'H001_ADMIN';
UPDATE admin SET real_name = '交大医院管理员' WHERE admin_id = 'H002_ADMIN';

-- 更新用户真实姓名为中文
UPDATE user SET real_name = '超级管理员' WHERE id = 'admin001';
