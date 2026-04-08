# 高考录取数据爬取项目

## 项目说明

本爬虫项目用于采集全国高考录取数据，包括：
- 院校信息（全国 2000+ 所院校）
- 专业信息（本科专业目录）
- 历年分数线（2021-2024）
- 各省批次控制线
- 一分一段表

## 数据源

| 数据源 | 说明 | 数据类型 |
|--------|------|----------|
| 阳光高考平台 | 教育部官方 | 院校、分数线、控制线 |
| 掌上高考 | 数据全面 | 院校、专业、分数线 |
| 各省教育考试院 | 省份官方 | 分数线、一分一段表 |

## 快速开始

### 1. 安装依赖

```bash
cd gaokao-crawler
pip install -r requirements.txt
```

### 2. 配置数据库

编辑 `config.yaml`，修改数据库连接信息：

```yaml
database:
  host: localhost
  port: 3306
  user: root
  password: your_password  # 修改为实际密码
  database: gaokao
```

### 3. 运行爬虫

**Windows:**
```bash
run.bat
```

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```

**命令行方式:**
```bash
# 爬取所有数据
python main.py -t all --export

# 仅爬取院校
python main.py -t university --export

# 仅爬取分数线
python main.py -t score --export

# 爬取指定省份分数线
python main.py -t score -p 河南 -y 2024 --export
```

## 命令参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `-c, --config` | 配置文件路径 | `-c config.yaml` |
| `-t, --type` | 数据类型 | `-t score` |
| `-s, --source` | 数据源 | `-s gaokao_com` |
| `-p, --province` | 省份 | `-p 河南` |
| `-y, --year` | 年份 | `-y 2024` |
| `--subject` | 科类 | `--subject 理科` |
| `--no-db` | 不保存到数据库 | `--no-db` |
| `--export` | 导出数据到文件 | `--export` |

## 目录结构

```
gaokao-crawler/
├── config.yaml          # 配置文件
├── main.py              # 主程序入口
├── requirements.txt     # 依赖包
├── run.bat              # Windows启动脚本
├── run.sh               # Linux/Mac启动脚本
├── crawlers/            # 爬虫模块
│   ├── __init__.py
│   ├── base.py          # 基础爬虫类
│   ├── gaokao_cn.py     # 阳光高考爬虫
│   └── gaokao_com.py    # 掌上高考爬虫
├── storage/             # 数据存储模块
│   ├── __init__.py
│   ├── database.py      # 数据库操作
│   └── export.py        # 数据导出
├── logs/                # 日志目录
└── data/                # 数据输出目录
    ├── raw/             # 原始数据
    └── processed/       # 处理后数据
```

## 数据输出

爬取的数据会保存到以下位置：

1. **数据库**：直接写入 MySQL 数据库
2. **JSON文件**：`data/processed/*.json`
3. **CSV文件**：`data/processed/*.csv`
4. **Excel文件**：`data/processed/*.xlsx`

## 注意事项

1. **请求频率**：已内置请求延迟，避免被封IP
2. **数据完整性**：建议分批次爬取，先爬院校再爬分数线
3. **数据去重**：入库时会自动处理重复数据
4. **代理设置**：如需代理，在 `config.yaml` 中配置

## 常见问题

### Q: 爬取速度慢？
A: 这是正常的，为了避免被封IP，设置了请求延迟。可以在 `config.yaml` 中调整 `delay` 参数。

### Q: 数据不完整？
A: 部分数据可能需要登录或付费才能获取，建议使用官方数据源。

### Q: 如何获取2024年最新数据？
A: 2024年录取数据通常在7-8月录取结束后公布，请关注各省教育考试院官网。

## 数据使用建议

1. **优先使用官方数据**：阳光高考平台数据最权威
2. **交叉验证**：多个数据源对比验证
3. **定期更新**：每年录取结束后更新数据
4. **数据备份**：定期备份数据库