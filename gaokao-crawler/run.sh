#!/bin/bash

# 高考录取数据爬取工具启动脚本

echo "============================================"
echo "   高考录取数据爬取工具"
echo "============================================"
echo ""

# 检查Python
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未安装Python，请先安装Python 3.8+"
    exit 1
fi

# 检查依赖
echo "[1/3] 检查依赖..."
if ! python3 -c "import requests" &> /dev/null; then
    echo "[安装] 正在安装依赖..."
    pip3 install -r requirements.txt
fi

# 检查配置文件
if [ ! -f "config.yaml" ]; then
    echo "[警告] 未找到配置文件，请修改 config.yaml"
fi

echo ""
echo "[2/3] 请选择操作:"
echo "  1. 爬取所有数据（院校+专业+分数线）"
echo "  2. 仅爬取院校数据"
echo "  3. 仅爬取专业数据"
echo "  4. 仅爬取分数线数据"
echo "  5. 爬取指定省份分数线"
echo "  0. 退出"
echo ""

read -p "请输入选项 (0-5): " choice

case $choice in
    1)
        echo ""
        echo "[3/3] 开始爬取所有数据..."
        python3 main.py -t all --export
        ;;
    2)
        echo ""
        echo "[3/3] 开始爬取院校数据..."
        python3 main.py -t university --export
        ;;
    3)
        echo ""
        echo "[3/3] 开始爬取专业数据..."
        python3 main.py -t major --export
        ;;
    4)
        echo ""
        echo "[3/3] 开始爬取分数线数据..."
        python3 main.py -t score --export
        ;;
    5)
        read -p "请输入省份名称（如：河南）: " province
        read -p "请输入年份（如：2024）: " year
        echo ""
        echo "[3/3] 开始爬取 $province $year年 分数线数据..."
        python3 main.py -t score -p "$province" -y "$year" --export
        ;;
    0)
        echo "退出"
        exit 0
        ;;
    *)
        echo "无效选项"
        exit 1
        ;;
esac

echo ""
echo "============================================"
echo "   操作完成"
echo "============================================"