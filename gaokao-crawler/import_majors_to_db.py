"""
导入专业数据到MySQL数据库
"""

import json
import pymysql
from datetime import datetime

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'gaokao',
    'charset': 'utf8mb4'
}

def import_majors():
    """导入专业数据"""
    # 读取数据
    with open('./data/majors.json', 'r', encoding='utf-8') as f:
        majors = json.load(f)
    
    # 连接数据库
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 清空表
        cursor.execute("DELETE FROM major")
        print("已清空 major 表")
        
        # 插入数据
        sql = """
        INSERT INTO major (id, name, code, category, sub_category, degree_type, duration, 
                          employment_rating, avg_salary, gender_ratio, create_time, update_time, deleted)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0)
        """
        
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        data = []
        for m in majors:
            data.append((
                m['id'],
                m['name'],
                m['code'],
                m['category'],
                m['sub_category'],
                m['degree_type'],
                m.get('duration', 4),
                m.get('employment_rating'),
                m.get('avg_salary'),
                m.get('gender_ratio'),
                now,
                now
            ))
        
        cursor.executemany(sql, data)
        conn.commit()
        
        print(f"成功导入 {len(data)} 条专业数据")
        
        # 验证
        cursor.execute("SELECT COUNT(*) FROM major")
        count = cursor.fetchone()[0]
        print(f"数据库中共有 {count} 条专业记录")
        
    except Exception as e:
        conn.rollback()
        print(f"导入失败: {e}")
        raise
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    import_majors()