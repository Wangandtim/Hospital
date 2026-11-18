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

try:
    with conn.cursor() as cursor:
        # 查询重复的排班（同一医生同一天同一时段）
        cursor.execute("""
            SELECT hospital_id, doctor_id, work_date, time_slot, COUNT(*) as count
            FROM schedule
            WHERE work_date = '2026-06-07'
            GROUP BY hospital_id, doctor_id, work_date, time_slot
            HAVING COUNT(*) > 1
        """)
        duplicates = cursor.fetchall()
        print(f"找到 {len(duplicates)} 组重复的排班")
        
        # 删除重复的排班（保留非COPY_开头的，删除COPY_开头的）
        delete_count = 0
        for dup in duplicates:
            hospital_id, doctor_id, work_date, time_slot, count = dup
            cursor.execute("""
                DELETE FROM schedule 
                WHERE hospital_id = %s AND doctor_id = %s AND work_date = %s AND time_slot = %s
                AND schedule_id LIKE 'COPY_%'
            """, (hospital_id, doctor_id, work_date, time_slot))
            delete_count += cursor.rowcount
        
        conn.commit()
        print(f"成功删除 {delete_count} 条重复的排班数据")
        
finally:
    conn.close()