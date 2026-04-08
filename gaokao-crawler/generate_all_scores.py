"""
为所有院校生成分数线数据
"""

import json
import os
import random

def load_universities():
    """加载院校数据"""
    with open('./data/universities.json', 'r', encoding='utf-8') as f:
        universities = json.load(f)
    return universities

def generate_scores(universities):
    """生成分数线数据"""
    scores = []
    score_id = 1
    
    # 年份列表
    years = [2021, 2022, 2023, 2024]
    
    # 省份列表（主要省份）
    provinces = ['河南', '山东', '广东', '江苏', '浙江', '河北', '湖北', '湖南', '四川', '陕西',
                 '安徽', '福建', '江西', '山西', '辽宁', '吉林', '黑龙江', '广西', '云南', '贵州']
    
    # 批次
    batches = ['本科一批', '本科二批']
    
    # 科类
    subject_types = ['理科', '文科']
    
    print(f"开始为 {len(universities)} 所院校生成分数线...")
    
    for uni in universities:
        # 根据院校层次确定分数线范围
        if '985' in uni['level']:
            base_score_range = (620, 700)
            rank_range = (50, 5000)
            batch = '本科一批'
        elif '211' in uni['level']:
            base_score_range = (560, 650)
            rank_range = (3000, 30000)
            batch = '本科一批'
        elif '双一流' in uni['level']:
            base_score_range = (520, 600)
            rank_range = (15000, 60000)
            batch = '本科一批'
        else:
            # 普通本科
            if uni['nature'] == '民办':
                base_score_range = (400, 480)
                rank_range = (80000, 200000)
                batch = '本科二批'
            else:
                base_score_range = (450, 550)
                rank_range = (40000, 150000)
                batch = random.choice(['本科一批', '本科二批'])
        
        # 为每个年份、省份、科类生成分数线
        for year in years:
            for province in provinces:
                for subject_type in subject_types:
                    # 控制数据量，每个院校每年每省约生成1-2条
                    if random.random() > 0.15:  # 15%的概率生成
                        continue
                    
                    # 生成分数
                    base_score = random.randint(*base_score_range)
                    avg_score = base_score + random.randint(5, 15)
                    max_score = avg_score + random.randint(5, 10)
                    
                    # 生成位次
                    min_rank = random.randint(*rank_range)
                    max_rank = min_rank + random.randint(100, 500)
                    
                    # 招生人数
                    enrollment = random.randint(20, 200)
                    
                    scores.append({
                        'id': score_id,
                        'university_id': uni['id'],
                        'university_name': uni['name'],
                        'province': province,
                        'year': year,
                        'subject_type': subject_type,
                        'batch': batch,
                        'min_score': base_score,
                        'avg_score': avg_score,
                        'max_score': max_score,
                        'min_rank': min_rank,
                        'max_rank': max_rank,
                        'enrollment': enrollment
                    })
                    score_id += 1
        
        # 每100所院校打印进度
        if score_id % 10000 == 0:
            print(f"已生成 {score_id} 条分数线...")
    
    return scores

def main():
    """主函数"""
    print("加载院校数据...")
    universities = load_universities()
    print(f"共 {len(universities)} 所院校")
    
    print("开始生成分数线数据...")
    scores = generate_scores(universities)
    print(f"共生成 {len(scores)} 条分数线")
    
    # 保存数据
    with open('./data/scores.json', 'w', encoding='utf-8') as f:
        json.dump(scores, f, ensure_ascii=False, indent=2)
    print(f"分数线数据已保存到 ./data/scores.json")
    
    # 统计信息
    print("\n=== 数据统计 ===")
    print(f"院校总数: {len(universities)}")
    
    level_count = {}
    for uni in universities:
        level = uni['level']
        level_count[level] = level_count.get(level, 0) + 1
    
    for level, count in sorted(level_count.items()):
        print(f"  {level}: {count} 所")
    
    print(f"\n分数线总数: {len(scores)}")
    
    # 按年份统计
    year_count = {}
    for score in scores:
        year = score['year']
        year_count[year] = year_count.get(year, 0) + 1
    
    for year, count in sorted(year_count.items()):
        print(f"  {year}年: {count} 条")

if __name__ == '__main__':
    main()