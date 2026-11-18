import pymysql
import uuid
from config.db_secrets import db_password

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password=db_password(),
    database='hospital_db'
)

try:
    with conn.cursor() as cursor:
        # 查询6月8日的排班数据
        cursor.execute("SELECT hospital_id, doctor_id, time_slot, max_patients, status FROM schedule WHERE work_date='2026-06-08';")
        schedules = cursor.fetchall()
        print(f"找到 {len(schedules)} 条6月8日的排班数据")
        
        # 复制到6月7日
        count = 0
        for schedule in schedules:
            hospital_id, doctor_id, time_slot, max_patients, status = schedule
            schedule_id = f"COPY_{uuid.uuid4().hex}"
            cursor.execute(
                "INSERT INTO schedule (schedule_id, hospital_id, doctor_id, work_date, time_slot, max_patients, booked_count, status) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                (schedule_id, hospital_id, doctor_id, '2026-06-07', time_slot, max_patients, 0, status)
            )
            count += 1
            if count % 1000 == 0:
                print(f"已复制 {count} 条数据")
        
        conn.commit()
        print(f"成功复制 {count} 条排班数据到2026-06-07")
        
finally:
    conn.close()