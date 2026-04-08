"""
高考录取数据爬取主程序
支持爬取院校、专业、分数线等数据
"""

import os
import sys
import json
import argparse
from datetime import datetime
from pathlib import Path

import yaml
from loguru import logger

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

from crawlers.gaokao_cn import GaokaoCNCrawler
from crawlers.gaokao_com import GaokaoComCrawler
from storage.database import DatabaseStorage
from storage.export import DataExporter


def setup_logging(config: dict):
    """配置日志"""
    log_config = config.get('logging', {})
    
    # 移除默认处理器
    logger.remove()
    
    # 添加控制台输出
    logger.add(
        sys.stdout,
        level=log_config.get('level', 'INFO'),
        format='<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>'
    )
    
    # 添加文件输出
    log_file = log_config.get('file', './logs/crawler.log')
    log_dir = Path(log_file).parent
    log_dir.mkdir(parents=True, exist_ok=True)
    
    logger.add(
        log_file,
        level=log_config.get('level', 'INFO'),
        rotation=log_config.get('rotation', '10 MB'),
        retention=log_config.get('retention', '7 days'),
        encoding='utf-8',
        format='{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}'
    )


def load_config(config_path: str = 'config.yaml') -> dict:
    """加载配置文件"""
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def crawl_universities(config: dict, source: str = 'gaokao_com') -> list:
    """
    爬取院校数据
    
    Args:
        config: 配置字典
        source: 数据源
        
    Returns:
        院校列表
    """
    logger.info(f"开始从 {source} 爬取院校数据...")
    
    if source == 'gaokao_cn':
        crawler = GaokaoCNCrawler(config)
    else:
        crawler = GaokaoComCrawler(config)
    
    universities = crawler.crawl_universities()
    crawler.close()
    
    logger.info(f"共爬取 {len(universities)} 所院校")
    return universities


def crawl_majors(config: dict, source: str = 'gaokao_com') -> list:
    """
    爬取专业数据
    
    Args:
        config: 配置字典
        source: 数据源
        
    Returns:
        专业列表
    """
    logger.info(f"开始从 {source} 爬取专业数据...")
    
    crawler = GaokaoComCrawler(config)
    majors = crawler.crawl_majors()
    crawler.close()
    
    logger.info(f"共爬取 {len(majors)} 个专业")
    return majors


def crawl_scores(config: dict, source: str = 'gaokao_com',
                 province: str = None, year: int = None,
                 subject_type: str = None) -> list:
    """
    爬取分数线数据
    
    Args:
        config: 配置字典
        source: 数据源
        province: 省份
        year: 年份
        subject_type: 科类
        
    Returns:
        分数线列表
    """
    logger.info(f"开始从 {source} 爬取分数线数据...")
    
    if source == 'gaokao_cn':
        crawler = GaokaoCNCrawler(config)
    else:
        crawler = GaokaoComCrawler(config)
    
    scores = crawler.crawl_scores(province=province, year=year, subject_type=subject_type)
    crawler.close()
    
    logger.info(f"共爬取 {len(scores)} 条分数线")
    return scores


def save_to_database(config: dict, universities: list = None, 
                     majors: list = None, scores: list = None):
    """
    保存数据到数据库
    
    Args:
        config: 配置字典
        universities: 院校列表
        majors: 专业列表
        scores: 分数线列表
    """
    storage = DatabaseStorage(config)
    
    if not storage.test_connection():
        logger.error("数据库连接失败，跳过保存")
        return
    
    if universities:
        count = storage.save_universities(universities)
        logger.info(f"已保存 {count} 条院校数据到数据库")
    
    if majors:
        count = storage.save_majors(majors)
        logger.info(f"已保存 {count} 条专业数据到数据库")
    
    if scores:
        count = storage.save_scores(scores)
        logger.info(f"已保存 {count} 条分数线数据到数据库")
    
    storage.close()


def export_data(config: dict, universities: list = None,
                majors: list = None, scores: list = None):
    """
    导出数据到文件
    
    Args:
        config: 配置字典
        universities: 院校列表
        majors: 专业列表
        scores: 分数线列表
    """
    output_config = config.get('output', {})
    output_dir = output_config.get('processed_dir', './data/processed')
    
    exporter = DataExporter(output_dir)
    
    if universities:
        results = exporter.export_universities(universities)
        logger.info(f"院校数据已导出: {results}")
    
    if majors:
        results = exporter.export_majors(majors)
        logger.info(f"专业数据已导出: {results}")
    
    if scores:
        results = exporter.export_scores(scores)
        logger.info(f"分数线数据已导出: {results}")
        
        # 按省份导出
        province_results = exporter.export_by_province(scores)
        logger.info(f"按省份导出完成: {len(province_results)} 个省份")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='高考录取数据爬取工具')
    
    parser.add_argument(
        '-c', '--config',
        default='config.yaml',
        help='配置文件路径'
    )
    
    parser.add_argument(
        '-t', '--type',
        choices=['all', 'university', 'major', 'score'],
        default='all',
        help='爬取数据类型'
    )
    
    parser.add_argument(
        '-s', '--source',
        choices=['gaokao_cn', 'gaokao_com'],
        default='gaokao_com',
        help='数据源'
    )
    
    parser.add_argument(
        '-p', '--province',
        help='省份名称（爬取分数线时使用）'
    )
    
    parser.add_argument(
        '-y', '--year',
        type=int,
        help='年份（爬取分数线时使用）'
    )
    
    parser.add_argument(
        '--subject',
        help='科类（理科/文科/物理类/历史类）'
    )
    
    parser.add_argument(
        '--no-db',
        action='store_true',
        help='不保存到数据库'
    )
    
    parser.add_argument(
        '--export',
        action='store_true',
        help='导出数据到文件'
    )
    
    args = parser.parse_args()
    
    # 加载配置
    config = load_config(args.config)
    
    # 配置日志
    setup_logging(config)
    
    logger.info("=" * 60)
    logger.info("高考录取数据爬取工具启动")
    logger.info(f"数据类型: {args.type}")
    logger.info(f"数据源: {args.source}")
    logger.info("=" * 60)
    
    # 爬取数据
    universities = None
    majors = None
    scores = None
    
    try:
        if args.type in ['all', 'university']:
            universities = crawl_universities(config, args.source)
        
        if args.type in ['all', 'major']:
            majors = crawl_majors(config, args.source)
        
        if args.type in ['all', 'score']:
            scores = crawl_scores(
                config, args.source,
                province=args.province,
                year=args.year,
                subject_type=args.subject
            )
        
        # 保存到数据库
        if not args.no_db:
            save_to_database(config, universities, majors, scores)
        
        # 导出数据
        if args.export:
            export_data(config, universities, majors, scores)
        
        logger.info("=" * 60)
        logger.info("数据爬取完成")
        logger.info(f"院校: {len(universities) if universities else 0} 所")
        logger.info(f"专业: {len(majors) if majors else 0} 个")
        logger.info(f"分数线: {len(scores) if scores else 0} 条")
        logger.info("=" * 60)
        
    except KeyboardInterrupt:
        logger.warning("用户中断爬取")
    except Exception as e:
        logger.error(f"爬取过程出错: {e}")
        raise


if __name__ == '__main__':
    main()