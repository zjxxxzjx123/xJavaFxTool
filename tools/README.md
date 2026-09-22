# 自动数据分析工具

`data_analyzer.py` 是一个零第三方依赖的 Python 命令行程序，用于自动分析 CSV 或 JSON（对象数组）数据并输出 JSON 报告。

## 功能

- 识别每列的数值或字符串类型；
- 统计行列数、缺失值、唯一值、数值列的最小值/最大值/均值/中位数；
- 列出字符串列最常见的五个值；
- 计算数值列两两之间的 Pearson 相关系数；
- 支持将结构化报告写入文件，方便后续自动化处理。

## 使用方法

```bash
python3 tools/data_analyzer.py data.csv
python3 tools/data_analyzer.py data.json --output report.json
```

CSV 应包含表头；JSON 必须为对象数组，例如：

```json
[{"name": "Ada", "score": 98}, {"name": "Lin", "score": 87}]
```
