@echo off
chcp 65001 >nul
echo ============================================
echo    高考录取数据爬取工具
echo ============================================
echo.

:: 检查Python
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未安装Python，请先安装Python 3.8+
    pause
    exit /b 1
)

:: 检查依赖
echo [1/3] 检查依赖...
pip show requests >nul 2>&1
if errorlevel 1 (
    echo [安装] 正在安装依赖...
    pip install -r requirements.txt
)

:: 检查配置文件
if not exist config.yaml (
    echo [警告] 未找到配置文件，请复制 config.example.yaml 为 config.yaml 并修改配置
    copy config.yaml config.yaml.bak >nul 2>&1
)

echo.
echo [2/3] 请选择操作:
echo   1. 爬取所有数据（院校+专业+分数线）
echo   2. 仅爬取院校数据
echo   3. 仅爬取专业数据
echo   4. 仅爬取分数线数据
echo   5. 爬取指定省份分数线
echo   6. 导出数据到Excel
echo   0. 退出
echo.

set /p choice="请输入选项 (0-6): "

if "%choice%"=="1" goto all
if "%choice%"=="2" goto university
if "%choice%"=="3" goto major
if "%choice%"=="4" goto score
if "%choice%"=="5" goto province_score
if "%choice%"=="6" goto export
if "%choice%"=="0" goto end

:all
echo.
echo [3/3] 开始爬取所有数据...
python main.py -t all --export
goto end

:university
echo.
echo [3/3] 开始爬取院校数据...
python main.py -t university --export
goto end

:major
echo.
echo [3/3] 开始爬取专业数据...
python main.py -t major --export
goto end

:score
echo.
echo [3/3] 开始爬取分数线数据...
python main.py -t score --export
goto end

:province_score
set /p province="请输入省份名称（如：河南）: "
set /p year="请输入年份（如：2024）: "
echo.
echo [3/3] 开始爬取 %province% %year%年 分数线数据...
python main.py -t score -p %province% -y %year% --export
goto end

:export
echo.
echo [3/3] 导出数据到Excel...
python -c "from storage.export import DataExporter; import json; from pathlib import Path; exporter = DataExporter(); print('请先爬取数据')"
goto end

:end
echo.
echo ============================================
echo    操作完成
echo ============================================
pause