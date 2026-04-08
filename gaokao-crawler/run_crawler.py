"""
运行爬虫脚本
"""

import json
import os
import sys

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from crawlers.web_crawler import GaokaoWebCrawler

def main():
    # 创建数据目录
    os.makedirs('data', exist_ok=True)
    
    crawler = GaokaoWebCrawler()
    
    # 爬取院校
    print('=' * 50)
    print('开始爬取院校数据...')
    print('=' * 50)
    universities = crawler.crawl_universities_from_gaokao_cn()
    print(f'共爬取 {len(universities)} 所院校')
    
    # 爬取分数线
    print('=' * 50)
    print('开始生成分数线数据...')
    print('=' * 50)
    scores = crawler.crawl_scores_from_static_data()
    print(f'共生成 {len(scores)} 条分数线')
    
    # 保存数据
    if universities:
        with open('./data/universities.json', 'w', encoding='utf-8') as f:
            json.dump(universities, f, ensure_ascii=False, indent=2)
        print(f'院校数据已保存到 ./data/universities.json')
    
    if scores:
        with open('./data/scores.json', 'w', encoding='utf-8') as f:
            json.dump(scores, f, ensure_ascii=False, indent=2)
        print(f'分数线数据已保存到 ./data/scores.json')
    
    crawler.close()
    
    print('=' * 50)
    print('爬取完成!')
    print('=' * 50)

if __name__ == '__main__':
    main()