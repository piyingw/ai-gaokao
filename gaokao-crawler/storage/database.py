"""
数据库存储模块
将爬取的数据存储到MySQL数据库
"""

import json
from typing import Dict, List, Any, Optional
from datetime import datetime

from loguru import logger
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.pool import QueuePool
import pandas as pd


class DatabaseStorage:
    """数据库存储类"""

    def __init__(self, config: Dict[str, Any]):
        """
        初始化数据库连接
        
        Args:
            config: 数据库配置
        """
        self.config = config
        self.engine = self._create_engine()

    def _create_engine(self) -> Engine:
        """创建数据库引擎"""
        db_config = self.config.get('database', {})
        
        url = f"mysql+pymysql://{db_config['user']}:{db_config['password']}@" \
              f"{db_config['host']}:{db_config['port']}/{db_config['database']}" \
              f"?charset={db_config.get('charset', 'utf8mb4')}"
        
        engine = create_engine(
            url,
            poolclass=QueuePool,
            pool_size=5,
            max_overflow=10,
            pool_timeout=30,
            pool_recycle=1800,
            echo=False
        )
        
        return engine

    def test_connection(self) -> bool:
        """测试数据库连接"""
        try:
            with self.engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("数据库连接成功")
            return True
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            return False

    def save_universities(self, universities: List[Dict], batch_size: int = 100) -> int:
        """
        保存院校数据
        
        Args:
            universities: 院校列表
            batch_size: 批量插入大小
            
        Returns:
            插入的记录数
        """
        if not universities:
            return 0
            
        df = pd.DataFrame(universities)
        
        # 数据清洗
        df = self._clean_university_data(df)
        
        # 去重
        df = df.drop_duplicates(subset=['id'], keep='last')
        
        # 插入数据库
        try:
            df.to_sql(
                'university',
                self.engine,
                if_exists='append',
                index=False,
                chunksize=batch_size,
                method='multi'
            )
            
            count = len(df)
            logger.info(f"成功保存 {count} 条院校数据")
            return count
            
        except Exception as e:
            logger.error(f"保存院校数据失败: {e}")
            # 尝试逐条插入
            return self._insert_universities_one_by_one(universities)

    def _clean_university_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """清洗院校数据"""
        # 重命名列
        column_mapping = {
            'id': 'id',
            'name': 'name',
            'code': 'code',
            'province': 'province',
            'city': 'city',
            'level': 'level',
            'type': 'type',
            'nature': 'nature',
            'ranking': 'ranking',
            'intro': 'intro',
            'features': 'features',
            'official_url': 'official_url',
            'admission_url': 'admission_url',
            'tags': 'tags'
        }
        
        # 只保留需要的列
        existing_cols = [col for col in column_mapping.keys() if col in df.columns]
        df = df[existing_cols]
        df = df.rename(columns={col: column_mapping[col] for col in existing_cols})
        
        # 处理空值
        df['ranking'] = pd.to_numeric(df['ranking'], errors='coerce')
        
        # 添加时间字段
        df['create_time'] = datetime.now()
        df['update_time'] = datetime.now()
        df['deleted'] = 0
        
        return df

    def _insert_universities_one_by_one(self, universities: List[Dict]) -> int:
        """逐条插入院校数据"""
        count = 0
        
        with self.engine.connect() as conn:
            for uni in universities:
                try:
                    sql = text("""
                        INSERT INTO university (id, name, code, province, city, level, type, nature, ranking, intro, features, tags, create_time, update_time, deleted)
                        VALUES (:id, :name, :code, :province, :city, :level, :type, :nature, :ranking, :intro, :features, :tags, NOW(), NOW(), 0)
                        ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        code = VALUES(code),
                        province = VALUES(province),
                        city = VALUES(city),
                        level = VALUES(level),
                        type = VALUES(type),
                        nature = VALUES(nature),
                        ranking = VALUES(ranking),
                        intro = VALUES(intro),
                        features = VALUES(features),
                        tags = VALUES(tags),
                        update_time = NOW()
                    """)
                    
                    conn.execute(sql, {
                        'id': uni.get('id'),
                        'name': uni.get('name'),
                        'code': uni.get('code'),
                        'province': uni.get('province'),
                        'city': uni.get('city'),
                        'level': uni.get('level'),
                        'type': uni.get('type'),
                        'nature': uni.get('nature'),
                        'ranking': uni.get('ranking'),
                        'intro': uni.get('intro'),
                        'features': uni.get('features'),
                        'tags': uni.get('tags')
                    })
                    conn.commit()
                    count += 1
                    
                except Exception as e:
                    logger.warning(f"插入院校失败: {uni.get('name')}, 错误: {e}")
                    
        return count

    def save_majors(self, majors: List[Dict], batch_size: int = 100) -> int:
        """
        保存专业数据
        
        Args:
            majors: 专业列表
            batch_size: 批量插入大小
            
        Returns:
            插入的记录数
        """
        if not majors:
            return 0
            
        df = pd.DataFrame(majors)
        df = self._clean_major_data(df)
        df = df.drop_duplicates(subset=['id'], keep='last')
        
        try:
            df.to_sql(
                'major',
                self.engine,
                if_exists='append',
                index=False,
                chunksize=batch_size
            )
            
            count = len(df)
            logger.info(f"成功保存 {count} 条专业数据")
            return count
            
        except Exception as e:
            logger.error(f"保存专业数据失败: {e}")
            return self._insert_majors_one_by_one(majors)

    def _clean_major_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """清洗专业数据"""
        column_mapping = {
            'id': 'id',
            'name': 'name',
            'code': 'code',
            'category': 'category',
            'sub_category': 'sub_category',
            'degree_type': 'degree_type',
            'duration': 'duration',
            'intro': 'intro',
            'courses': 'courses',
            'employment': 'employment'
        }
        
        existing_cols = [col for col in column_mapping.keys() if col in df.columns]
        df = df[existing_cols]
        df = df.rename(columns={col: column_mapping[col] for col in existing_cols})
        
        df['duration'] = pd.to_numeric(df['duration'], errors='coerce')
        df['create_time'] = datetime.now()
        df['update_time'] = datetime.now()
        df['deleted'] = 0
        
        return df

    def _insert_majors_one_by_one(self, majors: List[Dict]) -> int:
        """逐条插入专业数据"""
        count = 0
        
        with self.engine.connect() as conn:
            for major in majors:
                try:
                    sql = text("""
                        INSERT INTO major (id, name, code, category, sub_category, degree_type, duration, intro, create_time, update_time, deleted)
                        VALUES (:id, :name, :code, :category, :sub_category, :degree_type, :duration, :intro, NOW(), NOW(), 0)
                        ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        code = VALUES(code),
                        category = VALUES(category),
                        sub_category = VALUES(sub_category),
                        degree_type = VALUES(degree_type),
                        duration = VALUES(duration),
                        intro = VALUES(intro),
                        update_time = NOW()
                    """)
                    
                    conn.execute(sql, {
                        'id': major.get('id'),
                        'name': major.get('name'),
                        'code': major.get('code'),
                        'category': major.get('category'),
                        'sub_category': major.get('sub_category'),
                        'degree_type': major.get('degree_type'),
                        'duration': major.get('duration'),
                        'intro': major.get('intro')
                    })
                    conn.commit()
                    count += 1
                    
                except Exception as e:
                    logger.warning(f"插入专业失败: {major.get('name')}, 错误: {e}")
                    
        return count

    def save_scores(self, scores: List[Dict], batch_size: int = 500) -> int:
        """
        保存分数线数据
        
        Args:
            scores: 分数线列表
            batch_size: 批量插入大小
            
        Returns:
            插入的记录数
        """
        if not scores:
            return 0
            
        count = 0
        
        with self.engine.connect() as conn:
            for score in scores:
                try:
                    sql = text("""
                        INSERT INTO admission_score 
                        (university_id, major_id, province, year, batch, subject_type, 
                         min_score, avg_score, max_score, min_rank, max_rank, enrollment, admitted, create_time, update_time)
                        VALUES 
                        (:university_id, :major_id, :province, :year, :batch, :subject_type,
                         :min_score, :avg_score, :max_score, :min_rank, :max_rank, :enrollment, :admitted, NOW(), NOW())
                    """)
                    
                    conn.execute(sql, {
                        'university_id': score.get('university_id'),
                        'major_id': score.get('major_id'),
                        'province': score.get('province'),
                        'year': score.get('year'),
                        'batch': score.get('batch'),
                        'subject_type': score.get('subject_type'),
                        'min_score': score.get('min_score'),
                        'avg_score': score.get('avg_score'),
                        'max_score': score.get('max_score'),
                        'min_rank': score.get('min_rank'),
                        'max_rank': score.get('max_rank'),
                        'enrollment': score.get('enrollment'),
                        'admitted': score.get('admitted')
                    })
                    conn.commit()
                    count += 1
                    
                except Exception as e:
                    logger.debug(f"插入分数线失败: {score.get('university_name')}, 错误: {e}")
                    
        logger.info(f"成功保存 {count} 条分数线数据")
        return count

    def get_university_id_map(self) -> Dict[str, int]:
        """
        获取院校名称-ID映射
        
        Returns:
            院校名称到ID的映射字典
        """
        sql = "SELECT id, name FROM university WHERE deleted = 0"
        
        with self.engine.connect() as conn:
            result = conn.execute(text(sql))
            return {row[1]: row[0] for row in result}

    def clear_table(self, table_name: str, year: int = None):
        """
        清空表数据
        
        Args:
            table_name: 表名
            year: 年份（可选，只删除指定年份数据）
        """
        with self.engine.connect() as conn:
            if year:
                sql = f"DELETE FROM {table_name} WHERE year = {year}"
            else:
                sql = f"TRUNCATE TABLE {table_name}"
                
            conn.execute(text(sql))
            conn.commit()
            logger.info(f"已清空表 {table_name}")

    def close(self):
        """关闭数据库连接"""
        self.engine.dispose()