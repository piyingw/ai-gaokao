"""
爬虫模块初始化
"""

from crawlers.base import BaseCrawler
from crawlers.gaokao_cn import GaokaoCNCrawler
from crawlers.gaokao_com import GaokaoComCrawler

__all__ = [
    'BaseCrawler',
    'GaokaoCNCrawler',
    'GaokaoComCrawler'
]