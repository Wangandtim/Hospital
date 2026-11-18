import random
import uuid
import pymysql
from datetime import datetime, timedelta
from config.db_secrets import db_password

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': db_password(),
    'database': 'hospital_db',
    'charset': 'utf8mb4'
}

TIME_SLOTS = [
    "8:00-8:20", "8:20-8:40", "8:40-9:00",
    "9:00-9:20", "9:20-9:40", "9:40-10:00",
    "10:00-10:20", "10:20-10:40", "10:40-11:00",
    "11:00-11:20", "11:20-11:40", "11:40-12:00",
    "14:00-14:20", "14:20-14:40", "14:40-15:00",
    "15:00-15:20", "15:20-15:40", "15:40-16:00",
    "16:00-16:20", "16:20-16:40", "16:40-17:00",
    "17:00-17:20", "17:20-17:40", "17:40-18:00"
]

def get_slot_count_by_title(title):
    if not title:
        return 10
    title = title.lower()
    if '主任' in title:
        return 7
    elif '副主任' in title:
        return 9
    elif '主治' in title:
        return 11
    else:
        return 13

def select_random_slots(slots, count, seed):
    rng = random.Random(seed)
    morning = slots[:12]
    afternoon = slots[12:]
    
    morning_count = count // 2
    afternoon_count = count - morning_count
    
    selected = []
    
    temp_morning = morning.copy()
    for _ in range(min(morning_count, len(temp_morning))):
        idx = rng.randint(0, len(temp_morning)-1)
        selected.append(temp_morning.pop(idx))
    
    temp_afternoon = afternoon.copy()
    for _ in range(min(afternoon_count, len(temp_afternoon))):
        idx = rng.randint(0, len(temp_afternoon)-1)
        selected.append(temp_afternoon.pop(idx))
    
    remaining = count - len(selected)
    temp_all = [s for s in slots if s not in selected]
    for _ in range(min(remaining, len(temp_all))):
        idx = rng.randint(0, len(temp_all)-1)
        selected.append(temp_all.pop(idx))
    
    return sorted(selected)

def main():
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        cursor.execute("TRUNCATE TABLE schedule")
        print("已清空旧排班数据")
        
        cursor.execute("SELECT doctor_id, name, title, hospital_id FROM doctor")
        doctors = cursor.fetchall()
        
        print(f"发现 {len(doctors)} 位医生")
        
        today = datetime.now().date()
        
        total_schedules = 0
        batch_size = 1000
        batch_data = []
        
        insert_sql = """INSERT INTO schedule 
            (schedule_id, hospital_id, doctor_id, work_date, time_slot, max_patients, booked_count, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"""
        
        for i, doctor in enumerate(doctors):
            doctor_id, name, title, hospital_id = doctor
            slot_count = get_slot_count_by_title(title)
            
            if i % 50 == 0:
                print(f"处理进度: {i}/{len(doctors)}")
            
            for day_offset in range(7):
                work_date = today + timedelta(days=day_offset)
                date_str = work_date.strftime('%Y-%m-%d')
                
                seed = int(hash(f"{doctor_id}{day_offset}")) % (2**31)
                slots = select_random_slots(TIME_SLOTS, slot_count, seed)
                
                for slot in slots:
                    schedule_id = str(uuid.uuid4()).replace('-', '')
                    batch_data.append((schedule_id, hospital_id, doctor_id, date_str, slot, 10, 0, 1))
                    total_schedules += 1
                    
                    if len(batch_data) >= batch_size:
                        cursor.executemany(insert_sql, batch_data)
                        conn.commit()
                        batch_data = []
        
        if batch_data:
            cursor.executemany(insert_sql, batch_data)
            conn.commit()
        
        print(f"\n=== 完成 ===")
        print(f"共生成 {total_schedules} 条排班记录")
        
    except Exception as e:
        print(f"错误: {e}")
        if conn:
            conn.rollback()
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    main()
