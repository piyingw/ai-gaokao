"""
将生成的数据导入数据库
"""

import json
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from sqlalchemy import create_engine, text
from loguru import logger


def import_to_database():
    """导入数据到数据库"""
    
    # 数据库配置 - 请根据实际情况修改
    DB_CONFIG = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',  # 请修改为实际密码
        'database': 'gaokao'
    }
    
    # 创建数据库连接
    url = f"mysql+pymysql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}?charset=utf8mb4"
    engine = create_engine(url)
    
    # 测试连接
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        logger.info("数据库连接成功")
    except Exception as e:
        logger.error(f"数据库连接失败: {e}")
        logger.info("请检查数据库配置，确保MySQL服务已启动且密码正确")
        return
    
    # 读取数据
    with open('./data/universities.json', 'r', encoding='utf-8') as f:
        universities = json.load(f)
    
    with open('./data/scores.json', 'r', encoding='utf-8') as f:
        scores = json.load(f)
    
    # 导入院校数据
    logger.info(f"开始导入 {len(universities)} 所院校...")
    uni_count = 0
    
    with engine.connect() as conn:
        for uni in universities:
            try:
                sql = text("""
                    INSERT INTO university (id, name, code, province, city, level, type, nature, ranking, create_time, update_time, deleted)
                    VALUES (:id, :name, :code, :province, :city, :level, :type, :nature, :ranking, NOW(), NOW(), 0)
                    ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    code = VALUES(code),
                    province = VALUES(province),
                    city = VALUES(city),
                    level = VALUES(level),
                    type = VALUES(type),
                    nature = VALUES(nature),
                    ranking = VALUES(ranking),
                    update_time = NOW()
                """)
                
                conn.execute(sql, {
                    'id': uni['id'],
                    'name': uni['name'],
                    'code': uni['code'],
                    'province': uni['province'],
                    'city': uni['city'],
                    'level': uni['level'],
                    'type': uni['type'],
                    'nature': uni['nature'],
                    'ranking': uni['ranking']
                })
                conn.commit()
                uni_count += 1
                
            except Exception as e:
                logger.warning(f"导入院校失败: {uni['name']}, 错误: {e}")
    
    logger.info(f"成功导入 {uni_count} 所院校")
    
    # 导入分数线数据
    # 先清空已有数据
    with engine.connect() as conn:
        conn.execute(text("DELETE FROM admission_score"))
        conn.commit()
    logger.info("已清空原有分数线数据")
    
    logger.info(f"开始导入 {len(scores)} 条分数线...")
    score_count = 0
    
    with engine.connect() as conn:
        for score in scores:
            try:
                sql = text("""
                    INSERT INTO admission_score 
                    (id, university_id, province, year, batch, subject_type, 
                     min_score, avg_score, max_score, min_rank, max_rank, enrollment, create_time, update_time)
                    VALUES 
                    (:id, :university_id, :province, :year, :batch, :subject_type,
                     :min_score, :avg_score, :max_score, :min_rank, :max_rank, :enrollment, NOW(), NOW())
                """)
                
                conn.execute(sql, {
                    'id': score['id'],
                    'university_id': score['university_id'],
                    'province': score['province'],
                    'year': score['year'],
                    'batch': score['batch'],
                    'subject_type': score['subject_type'],
                    'min_score': score['min_score'],
                    'avg_score': score['avg_score'],
                    'max_score': score['max_score'],
                    'min_rank': score['min_rank'],
                    'max_rank': score['max_rank'],
                    'enrollment': score['enrollment']
                })
                conn.commit()
                score_count += 1
                
            except Exception as e:
                logger.warning(f"导入分数线失败: {score['university_name']}, 错误: {e}")
    
    logger.info(f"成功导入 {score_count} 条分数线")
    
    # 验证数据
    with engine.connect() as conn:
        uni_result = conn.execute(text("SELECT COUNT(*) FROM university WHERE deleted = 0"))
        uni_total = uni_result.scalar()
        
        score_result = conn.execute(text("SELECT COUNT(*) FROM admission_score"))
        score_total = score_result.scalar()
    
    logger.info("=" * 50)
    logger.info("数据导入完成!")
    logger.info(f"数据库中院校总数: {uni_total}")
    logger.info(f"数据库中分数线总数: {score_total}")
    logger.info("=" * 50)


if __name__ == '__main__':
    import_to_database()