"""
数据导出模块
支持导出为Excel、CSV、JSON等格式
"""

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any

import pandas as pd
from loguru import logger


class DataExporter:
    """数据导出类"""

    def __init__(self, output_dir: str = './data'):
        """
        初始化导出器
        
        Args:
            output_dir: 输出目录
        """
        self.output_dir = Path(output_dir)
        self.raw_dir = self.output_dir / 'raw'
        self.processed_dir = self.output_dir / 'processed'
        
        # 创建目录
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self.processed_dir.mkdir(parents=True, exist_ok=True)

    def export_to_json(self, data: List[Dict], filename: str, raw: bool = True) -> str:
        """
        导出为JSON文件
        
        Args:
            data: 数据列表
            filename: 文件名
            raw: 是否为原始数据
            
        Returns:
            文件路径
        """
        output_dir = self.raw_dir if raw else self.processed_dir
        filepath = output_dir / f"{filename}.json"
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            
        logger.info(f"已导出JSON文件: {filepath}")
        return str(filepath)

    def export_to_csv(self, data: List[Dict], filename: str, raw: bool = True) -> str:
        """
        导出为CSV文件
        
        Args:
            data: 数据列表
            filename: 文件名
            raw: 是否为原始数据
            
        Returns:
            文件路径
        """
        output_dir = self.raw_dir if raw else self.processed_dir
        filepath = output_dir / f"{filename}.csv"
        
        df = pd.DataFrame(data)
        df.to_csv(filepath, index=False, encoding='utf-8-sig')
        
        logger.info(f"已导出CSV文件: {filepath}")
        return str(filepath)

    def export_to_excel(self, data: List[Dict], filename: str, 
                        sheet_name: str = 'Sheet1', raw: bool = True) -> str:
        """
        导出为Excel文件
        
        Args:
            data: 数据列表
            filename: 文件名
            sheet_name: 工作表名
            raw: 是否为原始数据
            
        Returns:
            文件路径
        """
        output_dir = self.raw_dir if raw else self.processed_dir
        filepath = output_dir / f"{filename}.xlsx"
        
        df = pd.DataFrame(data)
        
        with pd.ExcelWriter(filepath, engine='openpyxl') as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False)
            
            # 自动调整列宽
            worksheet = writer.sheets[sheet_name]
            for idx, col in enumerate(df.columns):
                max_length = max(
                    df[col].astype(str).str.len().max(),
                    len(str(col))
                ) + 2
                worksheet.column_dimensions[chr(65 + idx)].width = min(max_length, 50)
        
        logger.info(f"已导出Excel文件: {filepath}")
        return str(filepath)

    def export_all(self, data: Dict[str, List[Dict]], 
                   formats: List[str] = ['json', 'csv', 'excel']) -> Dict[str, str]:
        """
        导出所有数据
        
        Args:
            data: 数据字典 {数据类型: 数据列表}
            formats: 导出格式列表
            
        Returns:
            导出的文件路径字典
        """
        results = {}
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        
        for data_type, items in data.items():
            if not items:
                continue
                
            filename = f"{data_type}_{timestamp}"
            
            if 'json' in formats:
                results[f"{data_type}_json"] = self.export_to_json(items, filename)
                
            if 'csv' in formats:
                results[f"{data_type}_csv"] = self.export_to_csv(items, filename)
                
            if 'excel' in formats:
                results[f"{data_type}_excel"] = self.export_to_excel(items, filename)
        
        return results

    def export_universities(self, universities: List[Dict]) -> Dict[str, str]:
        """导出院校数据"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f"universities_{timestamp}"
        
        results = {
            'json': self.export_to_json(universities, filename),
            'csv': self.export_to_csv(universities, filename),
            'excel': self.export_to_excel(universities, filename, '院校数据')
        }
        
        return results

    def export_scores(self, scores: List[Dict]) -> Dict[str, str]:
        """导出分数线数据"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f"scores_{timestamp}"
        
        results = {
            'json': self.export_to_json(scores, filename),
            'csv': self.export_to_csv(scores, filename),
            'excel': self.export_to_excel(scores, filename, '分数线数据')
        }
        
        return results

    def export_majors(self, majors: List[Dict]) -> Dict[str, str]:
        """导出专业数据"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f"majors_{timestamp}"
        
        results = {
            'json': self.export_to_json(majors, filename),
            'csv': self.export_to_csv(majors, filename),
            'excel': self.export_to_excel(majors, filename, '专业数据')
        }
        
        return results

    def export_by_province(self, scores: List[Dict]) -> Dict[str, str]:
        """
        按省份导出分数线数据
        
        Args:
            scores: 分数线列表
            
        Returns:
            导出的文件路径字典
        """
        # 按省份分组
        province_data = {}
        for score in scores:
            province = score.get('province', '未知')
            if province not in province_data:
                province_data[province] = []
            province_data[province].append(score)
        
        # 导出每个省份的数据
        results = {}
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        
        for province, items in province_data.items():
            filename = f"scores_{province}_{timestamp}"
            results[province] = self.export_to_excel(items, filename, province)
        
        return results

    def export_by_year(self, scores: List[Dict]) -> Dict[str, str]:
        """
        按年份导出分数线数据
        
        Args:
            scores: 分数线列表
            
        Returns:
            导出的文件路径字典
        """
        # 按年份分组
        year_data = {}
        for score in scores:
            year = score.get('year', '未知')
            if year not in year_data:
                year_data[year] = []
            year_data[year].append(score)
        
        # 导出每个年份的数据
        results = {}
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        
        for year, items in year_data.items():
            filename = f"scores_{year}_{timestamp}"
            results[str(year)] = self.export_to_excel(items, filename, f"{year}年")
        
        return results