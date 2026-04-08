"""
掌上高考爬虫
数据全面，包含院校、专业、分数线等
"""

import json
import time
from typing import Dict, List, Any, Optional
from urllib.parse import urlencode

from loguru import logger

from crawlers.base import BaseCrawler


class GaokaoComCrawler(BaseCrawler):
    """掌上高考爬虫"""

    # 省份ID映射
    PROVINCE_IDS = {
        '北京': 1, '天津': 2, '河北': 3, '山西': 4, '内蒙古': 5,
        '辽宁': 6, '吉林': 7, '黑龙江': 8,
        '上海': 9, '江苏': 10, '浙江': 11, '安徽': 12, '福建': 13,
        '江西': 14, '山东': 15,
        '河南': 16, '湖北': 17, '湖南': 18, '广东': 19, '广西': 20, '海南': 21,
        '重庆': 22, '四川': 23, '贵州': 24, '云南': 25, '西藏': 26,
        '陕西': 27, '甘肃': 28, '青海': 29, '宁夏': 30, '新疆': 31
    }

    # 科类ID映射
    SUBJECT_IDS = {
        '理科': '1', '文科': '2',
        '物理类': '1', '历史类': '2',
        '综合': '3'
    }

    # 院校类型ID
    TYPE_IDS = {
        '综合': '1', '理工': '2', '师范': '3', '医药': '4',
        '财经': '5', '政法': '6', '语言': '7', '农林': '8',
        '民族': '9', '军事': '10', '体育': '11', '艺术': '12'
    }

    # 院校层次ID
    LEVEL_IDS = {
        '985': '985',
        '211': '211',
        '双一流': 'dual_class',
        '普通': 'general'
    }

    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        self.base_url = config['sources']['gaokao_com'].get('base_url', 'https://api.eol.cn')
        
        # 掌上高考需要的特殊请求头
        self.extra_headers = {
            'Accept': 'application/json, text/plain, */*',
            'Origin': 'https://www.gaokao.cn',
            'Referer': 'https://www.gaokao.cn/',
        }

    def crawl(self, data_type: str = 'all', **kwargs) -> Dict[str, Any]:
        """爬取数据"""
        result = {}
        
        if data_type in ['all', 'university']:
            logger.info("开始爬取院校数据...")
            result['universities'] = self.crawl_universities(**kwargs)
            
        if data_type in ['all', 'major']:
            logger.info("开始爬取专业数据...")
            result['majors'] = self.crawl_majors(**kwargs)
            
        if data_type in ['all', 'score']:
            logger.info("开始爬取分数线数据...")
            result['scores'] = self.crawl_scores(**kwargs)
            
        return result

    def crawl_universities(self, **kwargs) -> List[Dict]:
        """
        爬取院校列表
        
        Returns:
            院校列表
        """
        universities = []
        
        # 使用新的API接口
        url = "https://api.eol.cn/web/api/"
        
        # 构建请求参数
        params = {
            'page': 1,
            'size': 50,
            'keyword': '',
            'province_id': '',
            'type_id': '',
            'level_id': '',
            'sort': 'rank'
        }
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Accept': 'application/json',
            'Referer': 'https://www.gaokao.cn/',
            'Origin': 'https://www.gaokao.cn'
        }
        
        page = 1
        while True:
            params['page'] = page
            
            try:
                response = self.session.get(url, params=params, headers=headers, timeout=30)
                
                if response.status_code != 200:
                    logger.warning(f"请求失败: {response.status_code}")
                    break
                    
                data = response.json()
                
                # 解析数据
                items = data.get('data', [])
                if not items:
                    items = data.get('item', [])
                    
                if not items:
                    logger.info(f"第 {page} 页无数据，停止爬取")
                    break
                    
                for item in items:
                    uni = self._parse_university_v2(item)
                    if uni:
                        universities.append(uni)
                    
                logger.info(f"已爬取第 {page} 页，累计 {len(universities)} 所院校")
                
                # 检查是否还有下一页
                if len(items) < 50:
                    break
                    
                page += 1
                time.sleep(0.5)
                
            except Exception as e:
                logger.error(f"爬取院校数据失败: {e}")
                break
                
        return universities
    
    def _parse_university_v2(self, item: Dict) -> Optional[Dict]:
        """解析单个院校（新版API）"""
        try:
            return {
                'id': item.get('school_id') or item.get('id'),
                'name': item.get('name') or item.get('school_name'),
                'code': item.get('code') or item.get('school_code'),
                'province': item.get('province_name') or item.get('province'),
                'city': item.get('city_name') or item.get('city'),
                'level': self._parse_level_v2(item),
                'type': item.get('type_name') or item.get('type'),
                'nature': item.get('nature_name') or item.get('nature'),
                'ranking': item.get('rank') or item.get('ranking'),
                'tags': json.dumps(item.get('tags', []), ensure_ascii=False),
                'source': 'gaokao_com'
            }
        except Exception as e:
            logger.debug(f"解析院校失败: {e}")
            return None
    
    def _parse_level_v2(self, item: Dict) -> str:
        """解析院校层次（新版）"""
        levels = []
        if item.get('f985') == 1 or item.get('f985') == '1':
            levels.append('985')
        if item.get('f211') == 1 or item.get('f211') == '1':
            levels.append('211')
        if item.get('dual_class') == 1 or item.get('dual_class') == '1':
            levels.append('双一流')
        return '/'.join(levels) if levels else '普通'

    def _parse_university(self, item: Dict) -> Dict:
        """解析单个院校"""
        return {
            'id': item.get('school_id'),
            'name': item.get('name'),
            'code': item.get('code'),
            'province': item.get('province_name'),
            'city': item.get('city_name'),
            'level': self._parse_level(item),
            'type': item.get('type_name'),
            'nature': item.get('nature_name'),
            'ranking': item.get('rank'),
            'tags': json.dumps(item.get('tags', []), ensure_ascii=False),
            'source': 'gaokao_com'
        }

    def _parse_level(self, item: Dict) -> str:
        """解析院校层次"""
        levels = []
        if item.get('f985') == '1':
            levels.append('985')
        if item.get('f211') == '1':
            levels.append('211')
        if item.get('dual_class') == '1':
            levels.append('双一流')
        return '/'.join(levels) if levels else '普通'

    def crawl_university_detail(self, university_id: int) -> Optional[Dict]:
        """
        爬取院校详情
        
        Args:
            university_id: 院校ID
            
        Returns:
            院校详情
        """
        url = f"{self.base_url}/gk/school/detail"
        
        params = {
            'school_id': university_id
        }
        
        response = self.get(url, params=params)
        if not response:
            return None
            
        try:
            data = response.json()
            if data.get('code') != 0:
                return None
                
            item = data.get('data', {})
            
            return {
                'id': item.get('school_id'),
                'name': item.get('name'),
                'code': item.get('code'),
                'province': item.get('province_name'),
                'city': item.get('city_name'),
                'level': self._parse_level(item),
                'type': item.get('type_name'),
                'nature': item.get('nature_name'),
                'ranking': item.get('rank'),
                'intro': item.get('content'),
                'features': json.dumps(item.get('special', []), ensure_ascii=False),
                'official_url': item.get('official_url'),
                'admission_url': item.get('admission_url'),
                'source': 'gaokao_com'
            }
            
        except Exception as e:
            logger.error(f"解析院校详情失败: {university_id}, 错误: {e}")
            return None

    def crawl_majors(self, category: str = None, **kwargs) -> List[Dict]:
        """
        爬取专业列表
        
        Args:
            category: 学科门类
            
        Returns:
            专业列表
        """
        majors = []
        page = 1
        page_size = 50
        
        while True:
            url = f"{self.base_url}/gk/major/list"
            
            params = {
                'page': page,
                'size': page_size,
                'category': category or '',
                'sub_category': ''
            }
            
            response = self.get(url, params=params)
            if not response:
                break
                
            try:
                data = response.json()
                
                if data.get('code') != 0:
                    break
                    
                items = data.get('data', {}).get('item', [])
                if not items:
                    break
                    
                for item in items:
                    major = self._parse_major(item)
                    majors.append(major)
                    
                logger.info(f"已爬取第 {page} 页专业，累计 {len(majors)} 个")
                
                # 检查是否还有下一页
                total = data.get('data', {}).get('numFound', 0)
                if page * page_size >= total:
                    break
                    
                page += 1
                
            except Exception as e:
                logger.error(f"解析专业数据失败: {e}")
                break
                
        return majors

    def _parse_major(self, item: Dict) -> Dict:
        """解析单个专业"""
        return {
            'id': item.get('special_id'),
            'name': item.get('name'),
            'code': item.get('code'),
            'category': item.get('category_name'),
            'sub_category': item.get('sub_name'),
            'degree_type': item.get('degree_name'),
            'duration': item.get('limit_year'),
            'intro': item.get('content'),
            'source': 'gaokao_com'
        }

    def crawl_scores(self, province: str = None, year: int = None,
                     subject_type: str = None, **kwargs) -> List[Dict]:
        """
        爬取分数线数据
        
        Args:
            province: 省份名称
            year: 年份
            subject_type: 科类
            
        Returns:
            分数线列表
        """
        scores = []
        
        # 获取要爬取的参数
        provinces = [province] if province else list(self.PROVINCE_IDS.keys())
        years = [year] if year else self.config.get('scope', {}).get('years', [2024])
        
        for prov in provinces:
            prov_id = self.PROVINCE_IDS.get(prov)
            if not prov_id:
                continue
                
            for y in years:
                for subject, subj_id in self.SUBJECT_IDS.items():
                    if subject_type and subject != subject_type:
                        continue
                        
                    try:
                        data = self._crawl_score_by_province(prov_id, y, subj_id)
                        if data:
                            for item in data:
                                item['province'] = prov
                                item['year'] = y
                                item['subject_type'] = subject
                            scores.extend(data)
                            
                        logger.info(f"已爬取 {prov} {y}年 {subject} 分数线 {len(data)} 条")
                        
                    except Exception as e:
                        logger.error(f"爬取分数线失败: {prov} {y} {subject}, 错误: {e}")
                        
        return scores

    def _crawl_score_by_province(self, province_id: int, year: int,
                                   subject_id: str) -> List[Dict]:
        """
        按省份爬取分数线
        
        Args:
            province_id: 省份ID
            year: 年份
            subject_id: 科类ID
            
        Returns:
            分数线列表
        """
        scores = []
        page = 1
        page_size = 20
        
        while True:
            url = f"{self.base_url}/gk/score/school"
            
            params = {
                'page': page,
                'size': page_size,
                'province_id': province_id,
                'year': year,
                'type': subject_id,
                'batch': ''
            }
            
            response = self.get(url, params=params)
            if not response:
                break
                
            try:
                data = response.json()
                
                if data.get('code') != 0:
                    break
                    
                items = data.get('data', {}).get('item', [])
                if not items:
                    break
                    
                for item in items:
                    score = self._parse_score(item)
                    scores.append(score)
                    
                # 检查是否还有下一页
                total = data.get('data', {}).get('numFound', 0)
                if page * page_size >= total:
                    break
                    
                page += 1
                
            except Exception as e:
                logger.error(f"解析分数线失败: {e}")
                break
                
        return scores

    def _parse_score(self, item: Dict) -> Dict:
        """解析单条分数线"""
        return {
            'university_id': item.get('school_id'),
            'university_name': item.get('name'),
            'batch': item.get('batch'),
            'min_score': item.get('min'),
            'avg_score': item.get('average'),
            'max_score': item.get('max'),
            'min_rank': item.get('rank'),
            'enrollment': item.get('num'),
            'source': 'gaokao_com'
        }

    def crawl_major_scores(self, university_id: int, province_id: int,
                           year: int, subject_id: str) -> List[Dict]:
        """
        爬取专业分数线
        
        Args:
            university_id: 院校ID
            province_id: 省份ID
            year: 年份
            subject_id: 科类ID
            
        Returns:
            专业分数线列表
        """
        url = f"{self.base_url}/gk/score/special"
        
        params = {
            'school_id': university_id,
            'province_id': province_id,
            'year': year,
            'type': subject_id
        }
        
        response = self.get(url, params=params)
        if not response:
            return []
            
        try:
            data = response.json()
            
            if data.get('code') != 0:
                return []
                
            scores = []
            for item in data.get('data', {}).get('item', []):
                score = {
                    'university_id': university_id,
                    'major_id': item.get('special_id'),
                    'major_name': item.get('name'),
                    'province': province_id,
                    'year': year,
                    'subject_type': subject_id,
                    'batch': item.get('batch'),
                    'min_score': item.get('min'),
                    'avg_score': item.get('average'),
                    'max_score': item.get('max'),
                    'min_rank': item.get('rank'),
                    'enrollment': item.get('num'),
                    'source': 'gaokao_com'
                }
                scores.append(score)
                
            return scores
            
        except Exception as e:
            logger.error(f"解析专业分数线失败: {e}")
            return []