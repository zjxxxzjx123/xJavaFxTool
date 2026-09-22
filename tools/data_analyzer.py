#!/usr/bin/env python3
"""Generate a compact, machine-readable profile for CSV or JSON tabular data."""

from __future__ import annotations

import argparse
import csv
import json
import math
import statistics
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable


MISSING_VALUES = {"", "na", "n/a", "null", "none", "nan", "-"}


def is_missing(value: Any) -> bool:
    return value is None or (isinstance(value, str) and value.strip().lower() in MISSING_VALUES)


def parse_number(value: Any) -> float | None:
    if isinstance(value, bool) or is_missing(value):
        return None
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    return number if math.isfinite(number) else None


def load_rows(path: Path, encoding: str) -> list[dict[str, Any]]:
    if path.suffix.lower() == ".csv":
        with path.open("r", encoding=encoding, newline="") as source:
            return list(csv.DictReader(source))
    if path.suffix.lower() == ".json":
        with path.open("r", encoding=encoding) as source:
            data = json.load(source)
        if not isinstance(data, list) or not all(isinstance(row, dict) for row in data):
            raise ValueError("JSON input must be an array of objects.")
        return data
    raise ValueError("Only .csv and .json inputs are supported.")


def median(values: list[float]) -> float:
    return round(statistics.median(values), 6)


def describe_column(values: Iterable[Any], row_count: int) -> dict[str, Any]:
    values = list(values)
    non_missing = [value for value in values if not is_missing(value)]
    numeric_values = [parse_number(value) for value in non_missing]
    numeric = [value for value in numeric_values if value is not None]
    result: dict[str, Any] = {
        "missing_count": row_count - len(non_missing),
        "missing_rate": round((row_count - len(non_missing)) / row_count, 6) if row_count else 0,
        "unique_count": len({str(value) for value in non_missing}),
    }
    if non_missing and len(numeric) == len(non_missing):
        result.update({
            "type": "number",
            "min": round(min(numeric), 6),
            "max": round(max(numeric), 6),
            "mean": round(statistics.fmean(numeric), 6),
            "median": median(numeric),
        })
    else:
        counts = Counter(str(value) for value in non_missing)
        result.update({
            "type": "string",
            "top_values": [
                {"value": value, "count": count}
                for value, count in counts.most_common(5)
            ],
        })
    return result


def pearson(left: list[float], right: list[float]) -> float | None:
    if len(left) < 2:
        return None
    left_mean, right_mean = statistics.fmean(left), statistics.fmean(right)
    numerator = sum((x - left_mean) * (y - right_mean) for x, y in zip(left, right))
    left_scale = math.sqrt(sum((x - left_mean) ** 2 for x in left))
    right_scale = math.sqrt(sum((y - right_mean) ** 2 for y in right))
    if not left_scale or not right_scale:
        return None
    return round(numerator / (left_scale * right_scale), 6)


def analyze(rows: list[dict[str, Any]]) -> dict[str, Any]:
    columns = sorted({key for row in rows for key in row})
    summaries = {column: describe_column((row.get(column) for row in rows), len(rows)) for column in columns}
    numeric_columns = [column for column in columns if summaries[column]["type"] == "number"]
    correlations = []
    for index, left_name in enumerate(numeric_columns):
        for right_name in numeric_columns[index + 1 :]:
            pairs = [
                (parse_number(row.get(left_name)), parse_number(row.get(right_name)))
                for row in rows
            ]
            complete_pairs = [(left, right) for left, right in pairs if left is not None and right is not None]
            coefficient = pearson([pair[0] for pair in complete_pairs], [pair[1] for pair in complete_pairs])
            if coefficient is not None:
                correlations.append({"columns": [left_name, right_name], "coefficient": coefficient,
                                     "sample_size": len(complete_pairs)})
    return {
        "row_count": len(rows),
        "column_count": len(columns),
        "columns": summaries,
        "correlations": correlations,
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Automatically profile CSV or JSON tabular data.")
    parser.add_argument("input", type=Path, help="Path to a CSV or JSON file")
    parser.add_argument("-o", "--output", type=Path, help="Write the JSON report to this file")
    parser.add_argument("--encoding", default="utf-8-sig", help="Input text encoding (default: utf-8-sig)")
    args = parser.parse_args()
    report = analyze(load_rows(args.input, args.encoding))
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
