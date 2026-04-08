"""
基础爬虫类
提供通用的HTTP请求、重试、代理等功能
"""

import time
import random
import hashlib
from abc import ABC, abstractmethod
from typing import Optional, Dict, Any, List
from urllib.parse import urljoin, urlencode

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from fake_useragent import UserAgent
from loguru import logger


class BaseCrawler(ABC):
    """基础爬虫类"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.session = self._create_session()
        self.ua = UserAgent()
        
        # 请求配置
        self.delay = config.get('crawler', {}).get('delay', 1.0)
        self.timeout = config.get('crawler', {}).get('timeout', 30)
        self.retry = config.get('crawler', {}).get('retry', 3)
        
        # 代理配置
        self.use_proxy = config.get('crawler', {}).get('use_proxy', False)
        self.proxies = config.get('crawler', {}).get('proxy', {})

    def _create_session(self) -> requests.Session:
        """创建带重试机制的Session"""
        session = requests.Session()
        
        # 配置重试策略
        retry_strategy = Retry(
            total=self.config.get('crawler', {}).get('retry', 3),
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["HEAD", "GET", "OPTIONS", "POST"]
        )
        
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        return session

    def _get_headers(self, url: str = None) -> Dict[str, str]:
        """生成请求头"""
        headers = {
            'User-Agent': self.ua.random,
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Cache-Control': 'max-age=0',
        }
        
        if url:
            headers['Referer'] = url
            
        return headers

    def _get_proxies(self) -> Optional[Dict[str, str]]:
        """获取代理配置"""
        if self.use_proxy and self.proxies:
            return self.proxies
        return None

    def request(self, method: str, url: str, **kwargs) -> Optional[requests.Response]:
        """
        发送HTTP请求
        
        Args:
            method: 请求方法 GET/POST
            url: 请求URL
            **kwargs: 其他请求参数
            
        Returns:
            Response对象或None
        """
        kwargs.setdefault('headers', self._get_headers(url))
        kwargs.setdefault('timeout', self.timeout)
        kwargs.setdefault('proxies', self._get_proxies())
        
        for attempt in range(self.retry):
            try:
                # 随机延迟
                time.sleep(self.delay + random.random())
                
                response = self.session.request(method, url, **kwargs)
                response.raise_for_status()
                
                logger.debug(f"请求成功: {url}")
                return response
                
            except requests.exceptions.RequestException as e:
                logger.warning(f"请求失败 (尝试 {attempt + 1}/{self.retry}): {url}, 错误: {e}")
                
                if attempt < self.retry - 1:
                    time.sleep(2 ** attempt)  # 指数退避
                else:
                    logger.error(f"请求最终失败: {url}")
                    return None

    def get(self, url: str, params: Dict = None, **kwargs) -> Optional[requests.Response]:
        """GET请求"""
        if params:
            url = f"{url}?{urlencode(params)}"
        return self.request('GET', url, **kwargs)

    def post(self, url: str, data: Dict = None, json: Dict = None, **kwargs) -> Optional[requests.Response]:
        """POST请求"""
        if json:
            kwargs['json'] = json
        elif data:
            kwargs['data'] = data
        return self.request('POST', url, **kwargs)

    def get_json(self, url: str, **kwargs) -> Optional[Dict]:
        """获取JSON数据"""
        response = self.get(url, **kwargs)
        if response:
            try:
                return response.json()
            except Exception as e:
                logger.error(f"JSON解析失败: {url}, 错误: {e}")
        return None

    def download_file(self, url: str, filepath: str) -> bool:
        """
        下载文件
        
        Args:
            url: 文件URL
            filepath: 保存路径
            
        Returns:
            是否成功
        """
        response = self.get(url, stream=True)
        if response:
            try:
                with open(filepath, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        f.write(chunk)
                logger.info(f"文件下载成功: {filepath}")
                return True
            except Exception as e:
                logger.error(f"文件保存失败: {filepath}, 错误: {e}")
        return False

    @staticmethod
    def md5(text: str) -> str:
        """计算MD5"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()

    @abstractmethod
    def crawl(self, *args, **kwargs) -> Any:
        """爬取数据（子类实现）"""
        pass

    def close(self):
        """关闭Session"""
        self.session.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()