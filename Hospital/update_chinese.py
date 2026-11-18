import os
import pymysql
from config.db_secrets import db_password

# 连接数据库（端口统一 3306，可通过环境变量覆盖）
conn = pymysql.connect(
    host=os.environ.get('DB_HOST', 'localhost'),
    port=int(os.environ.get('DB_PORT', '3306')),
    user=os.environ.get('DB_USER', 'root'),
    password=db_password(),
    database=os.environ.get('DB_NAME', 'hospital_db'),
    charset='utf8mb4'
)

cursor = conn.cursor()

try:
    # 更新医院名称
    hospitals = [
        ('H001', '邛崃市医疗中心医院', '四川省邛崃市文君街'),
        ('H002', '西安交通大学第一附属医院', '西安市雁塔区雁塔西路'),
        ('H003', '西京医院', '西安市新城区长乐西路'),
        ('H004', '陕西省人民医院', '西安市碑林区友谊西路'),
        ('H005', '四川大学华西医院', '成都市武侯区国学巷'),
        ('H006', '西安医学院第二附属医院', '西安市新城区尚勤路'),
        ('H007', '西安市中心医院', '西安市新城区西五路'),
        ('H008', '西安市第一医院', '西安市碑林区南大街'),
        ('H009', '西安市第三医院', '西安市未央区凤城三路东段'),
        ('H010', '西北妇女儿童医院', '西安市雁塔区雁翔路')
    ]
    
    for hid, name, addr in hospitals:
        sql = "UPDATE hospital SET hospital_name = %s, address = %s WHERE hospital_id = %s"
        cursor.execute(sql, (name, addr, hid))
        print(f"Updated hospital: {hid} -> {name}")
    
    # 更新科室名称
    departments = [
        ('DEPT001', '内科', '呼吸、消化、心血管疾病', '周一至周日 08:00-17:00'),
        ('DEPT002', '外科', '普通外科、骨科、神经外科', '周一至周日 08:00-17:00'),
        ('DEPT003', '儿科', '儿童疾病、预防接种', '周一至周日 08:00-18:00'),
        ('DEPT004', '妇产科', '妇科、产前检查、分娩', '周一至周日 08:00-17:00'),
        ('DEPT005', '急诊科', '24小时急诊服务', '24小时'),
        ('DEPT006', '眼科', '眼科疾病、视力检查', '周一至周六 08:00-17:00'),
        ('DEPT007', '口腔科', '口腔护理、种植牙、正畸', '周一至周六 08:00-17:00'),
        ('DEPT008', '皮肤科', '皮肤病、性病、皮肤科', '周一至周六 08:00-17:00')
    ]
    
    for did, name, desc, service in departments:
        sql = "UPDATE department SET name = %s, description = %s, service_time = %s WHERE id = %s"
        cursor.execute(sql, (name, desc, service, did))
        print(f"Updated department: {did} -> {name}")
    
    # 更新医生信息
    doctors = [
        ('DOC001', '张建国', '主任医师', '心血管疾病、高血压'),
        ('DOC002', '李明华', '副主任医师', '消化系统疾病、胃病'),
        ('DOC003', '王芳', '主治医师', '呼吸系统疾病、肺炎'),
        ('DOC004', '赵伟', '主任医师', '普通外科、胃肠外科'),
        ('DOC005', '陈静', '副主任医师', '骨科、关节置换'),
        ('DOC006', '刘洋', '主治医师', '创伤、急诊外科'),
        ('DOC007', '周小红', '主任医师', '小儿呼吸、新生儿科'),
        ('DOC008', '吴丽娟', '副主任医师', '儿童保健、预防接种'),
        ('DOC009', '孙梅', '主任医师', '妇科肿瘤、宫腔镜'),
        ('DOC010', '郑颖', '副主任医师', '产前检查、高危妊娠'),
        ('DOC011', '钱进', '主任医师', '急诊医学、重症监护'),
        ('DOC012', '马小东', '副主任医师', '急性中毒、创伤'),
        ('DOC013', '徐光明', '主任医师', '白内障、青光眼'),
        ('DOC014', '郭婷婷', '副主任医师', '种植牙、正畸'),
        ('DOC015', '韩雪', '主治医师', '过敏性皮肤病、湿疹')
    ]
    
    for did, name, title, expertise in doctors:
        sql = "UPDATE doctor SET name = %s, title = %s, expertise = %s WHERE doctor_id = %s"
        cursor.execute(sql, (name, title, expertise, did))
        print(f"Updated doctor: {did} -> {name}")
    
    # 更新管理员信息
    admins = [
        ('S001', '超级管理员'),
        ('H001_ADMIN', '邛崃医院管理员'),
        ('H002_ADMIN', '交大医院管理员')
    ]
    
    for aid, name in admins:
        sql = "UPDATE admin SET real_name = %s WHERE admin_id = %s"
        cursor.execute(sql, (name, aid))
        print(f"Updated admin: {aid} -> {name}")
    
    # 更新用户信息
    sql = "UPDATE user SET real_name = %s WHERE id = %s"
    cursor.execute(sql, ('超级管理员', 'admin001'))
    print("Updated user: admin001 -> 超级管理员")
    
    conn.commit()
    print("\nAll data updated successfully!")
    
except Exception as e:
    print(f"Error: {e}")
    conn.rollback()
finally:
    cursor.close()
    conn.close()
