"""
阳光高考平台爬虫
教育部官方数据源，数据权威可靠
"""

import re
import json
from typing import Dict, List, Any, Optional
from urllib.parse import urljoin, quote

from bs4 import BeautifulSoup
from loguru import logger

from crawlers.base import BaseCrawler


class GaokaoCNCrawler(BaseCrawler):
    """阳光高考平台爬虫"""

    # 省份代码映射
    PROVINCE_CODES = {
        '北京': '11', '天津': '12', '河北': '13', '山西': '14', '内蒙古': '15',
        '辽宁': '21', '吉林': '22', '黑龙江': '23',
        '上海': '31', '江苏': '32', '浙江': '33', '安徽': '34', '福建': '35',
        '江西': '36', '山东': '37',
        '河南': '41', '湖北': '42', '湖南': '43', '广东': '44', '广西': '45', '海南': '46',
        '重庆': '50', '四川': '51', '贵州': '52', '云南': '53', '西藏': '54',
        '陕西': '61', '甘肃': '62', '青海': '63', '宁夏': '64', '新疆': '65'
    }

    # 科类代码映射
    SUBJECT_CODES = {
        '理科': '1', '文科': '2',
        '物理类': '1', '历史类': '2',
        '综合': '3'
    }

    # 批次代码映射
    BATCH_CODES = {
        '本科提前批': '1', '本科一批': '2', '本科二批': '3',
        '本科批': '4', '专科批': '5', '高职专科批': '5'
    }

    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        self.base_url = config['sources']['gaokao_cn']['base_url']

    def crawl(self, data_type: str = 'all', **kwargs) -> Dict[str, Any]:
        """
        爬取数据
        
        Args:
            data_type: 数据类型 all/university/score/control_line
            **kwargs: 其他参数
            
        Returns:
            爬取的数据
        """
        result = {}
        
        if data_type in ['all', 'university']:
            logger.info("开始爬取院校数据...")
            result['universities'] = self.crawl_universities(**kwargs)
            
        if data_type in ['all', 'score']:
            logger.info("开始爬取分数线数据...")
            result['scores'] = self.crawl_scores(**kwargs)
            
        if data_type in ['all', 'control_line']:
            logger.info("开始爬取批次控制线...")
            result['control_lines'] = self.crawl_control_lines(**kwargs)
            
        return result

    def crawl_universities(self, **kwargs) -> List[Dict]:
        """
        爬取院校列表
        
        Returns:
            院校列表
        """
        universities = []
        page = 1
        
        while True:
            url = f"{self.base_url}/zsgs/zhangcheng/listZszc--method-index,ssdm-,start-{(page-1)*20}.dhtml"
            
            response = self.get(url)
            if not response:
                break
                
            soup = BeautifulSoup(response.text, 'lxml')
            
            # 解析院校列表
            items = soup.select('.zs-list-box .zs-list-item')
            if not items:
                # 尝试其他选择器
                items = soup.select('.yxk-table tbody tr')
            
            if not items:
                logger.info(f"第 {page} 页无数据，停止爬取")
                break
                
            for item in items:
                try:
                    uni = self._parse_university_item(item)
                    if uni:
                        universities.append(uni)
                except Exception as e:
                    logger.warning(f"解析院校失败: {e}")
                    
            logger.info(f"已爬取第 {page} 页，累计 {len(universities)} 所院校")
            page += 1
            
            # 限制页数（测试用）
            if page > 200:
                break
                
        return universities

    def _parse_university_item(self, item) -> Optional[Dict]:
        """解析单个院校信息"""
        try:
            # 尝试多种解析方式
            name_elem = item.select_one('.zs-list-title a, .yxk-table td:first-child a, a')
            if not name_elem:
                return None
                
            name = name_elem.get_text(strip=True)
            link = name_elem.get('href', '')
            
            # 提取院校ID
            uni_id = None
            if link:
                match = re.search(r'schId-(\d+)', link)
                if match:
                    uni_id = match.group(1)
                    
            # 解析其他信息
            info = {}
            tds = item.select('td')
            if len(tds) >= 4:
                info['code'] = tds[1].get_text(strip=True) if len(tds) > 1 else ''
                info['province'] = tds[2].get_text(strip=True) if len(tds) > 2 else ''
                info['level'] = tds[3].get_text(strip=True) if len(tds) > 3 else ''
            
            return {
                'id': uni_id,
                'name': name,
                'code': info.get('code', ''),
                'province': info.get('province', ''),
                'level': info.get('level', ''),
                'source': 'gaokao_cn'
            }
            
        except Exception as e:
            logger.debug(f"解析院校项失败: {e}")
            return None

    def crawl_university_detail(self, university_id: str) -> Optional[Dict]:
        """
        爬取院校详情
        
        Args:
            university_id: 院校ID
            
        Returns:
            院校详情
        """
        url = f"{self.base_url}/zsgs/zhangcheng/listZszc--schId-{university_id},method-view.dhtml"
        
        response = self.get(url)
        if not response:
            return None
            
        soup = BeautifulSoup(response.text, 'lxml')
        
        detail = {'id': university_id}
        
        try:
            # 解析基本信息
            info_items = soup.select('.zs-xx-box .zs-xx-item, .xxk-table tr')
            for item in info_items:
                label_elem = item.select_one('.zs-xx-label, td:first-child')
                value_elem = item.select_one('.zs-xx-value, td:last-child')
                
                if label_elem and value_elem:
                    label = label_elem.get_text(strip=True)
                    value = value_elem.get_text(strip=True)
                    
                    if '院校名称' in label:
                        detail['name'] = value
                    elif '院校代码' in label:
                        detail['code'] = value
                    elif '所在省市' in label:
                        detail['province'] = value
                    elif '院校层次' in label or '办学层次' in label:
                        detail['level'] = value
                    elif '院校类型' in label or '办学类型' in label:
                        detail['type'] = value
                    elif '院校简介' in label:
                        detail['intro'] = value
                        
        except Exception as e:
            logger.warning(f"解析院校详情失败: {university_id}, 错误: {e}")
            
        return detail

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
        
        # 获取要爬取的省份列表
        provinces = [province] if province else list(self.PROVINCE_CODES.keys())
        years = [year] if year else self.config.get('scope', {}).get('years', [2024])
        
        for prov in provinces:
            prov_code = self.PROVINCE_CODES.get(prov)
            if not prov_code:
                continue
                
            for y in years:
                for subject, subj_code in self.SUBJECT_CODES.items():
                    if subject_type and subject != subject_type:
                        continue
                        
                    try:
                        data = self._crawl_score_by_province(prov_code, y, subj_code)
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

    def _crawl_score_by_province(self, province_code: str, year: int, 
                                   subject_code: str) -> List[Dict]:
        """
        按省份爬取分数线
        
        Args:
            province_code: 省份代码
            year: 年份
            subject_code: 科类代码
            
        Returns:
            分数线列表
        """
        url = f"{self.base_url}/z/gk/fsx/queryFsx"
        
        params = {
            'ssdm': province_code,
            'year': year,
            'kl': subject_code
        }
        
        response = self.get(url, params=params)
        if not response:
            return []
            
        try:
            data = response.json()
            return self._parse_score_data(data)
        except Exception as e:
            logger.debug(f"解析分数线JSON失败: {e}")
            return []

    def _parse_score_data(self, data: Dict) -> List[Dict]:
        """解析分数线数据"""
        scores = []
        
        if not data or 'data' not in data:
            return scores
            
        for item in data.get('data', []):
            try:
                score = {
                    'university_id': item.get('schId'),
                    'university_name': item.get('schName'),
                    'min_score': item.get('minScore'),
                    'avg_score': item.get('avgScore'),
                    'max_score': item.get('maxScore'),
                    'min_rank': item.get('minRank'),
                    'batch': item.get('pc'),
                    'enrollment': item.get('zsrs'),
                    'source': 'gaokao_cn'
                }
                scores.append(score)
            except Exception as e:
                logger.debug(f"解析分数线项失败: {e}")
                
        return scores

    def crawl_control_lines(self, year: int = None) -> List[Dict]:
        """
        爬取各省批次控制线
        
        Args:
            year: 年份
            
        Returns:
            控制线列表
        """
        control_lines = []
        years = [year] if year else self.config.get('scope', {}).get('years', [2024])
        
        for y in years:
            for prov_name, prov_code in self.PROVINCE_CODES.items():
                try:
                    data = self._crawl_control_line(prov_code, y)
                    if data:
                        for item in data:
                            item['province'] = prov_name
                            item['year'] = y
                        control_lines.extend(data)
                        
                    logger.info(f"已爬取 {prov_name} {y}年 批次控制线")
                    
                except Exception as e:
                    logger.error(f"爬取控制线失败: {prov_name} {y}, 错误: {e}")
                    
        return control_lines

    def _crawl_control_line(self, province_code: str, year: int) -> List[Dict]:
        """爬取单个省份的批次控制线"""
        url = f"{self.base_url}/z/gk/fsx/queryControlLine"
        
        params = {
            'ssdm': province_code,
            'year': year
        }
        
        response = self.get(url, params=params)
        if not response:
            return []
            
        try:
            data = response.json()
            lines = []
            
            for item in data.get('data', []):
                line = {
                    'batch': item.get('pc'),
                    'subject_type': item.get('kl'),
                    'score': item.get('fsx'),
                    'source': 'gaokao_cn'
                }
                lines.append(line)
                
            return lines
            
        except Exception as e:
            logger.debug(f"解析控制线失败: {e}")
            return []

    def crawl_score_distribution(self, province: str, year: int, 
                                  subject_type: str = '理科') -> List[Dict]:
        """
        爬取一分一段表
        
        Args:
            province: 省份名称
            year: 年份
            subject_type: 科类
            
        Returns:
            一分一段表数据
        """
        prov_code = self.PROVINCE_CODES.get(province)
        subj_code = self.SUBJECT_CODES.get(subject_type)
        
        if not prov_code or not subj_code:
            logger.error(f"无效的省份或科类: {province}, {subject_type}")
            return []
            
        url = f"{self.base_url}/z/gk/fsx/queryYfyd"
        
        params = {
            'ssdm': prov_code,
            'year': year,
            'kl': subj_code
        }
        
        response = self.get(url, params=params)
        if not response:
            return []
            
        try:
            data = response.json()
            distribution = []
            
            for item in data.get('data', []):
                dist = {
                    'score': item.get('fs'),
                    'rank': item.get('pm'),
                    'count': item.get('rs'),
                    'province': province,
                    'year': year,
                    'subject_type': subject_type,
                    'source': 'gaokao_cn'
                }
                distribution.append(dist)
                
            return distribution
            
        except Exception as e:
            logger.error(f"解析一分一段表失败: {e}")
            return []