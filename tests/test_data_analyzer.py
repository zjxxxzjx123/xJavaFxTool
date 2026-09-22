import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).parents[1] / "tools" / "data_analyzer.py"
SPEC = importlib.util.spec_from_file_location("data_analyzer", MODULE_PATH)
data_analyzer = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(data_analyzer)


class DataAnalyzerTest(unittest.TestCase):
    def test_analyze_profiles_missing_values_and_correlations(self):
        report = data_analyzer.analyze([
            {"name": "Ada", "score": "10", "hours": "1"},
            {"name": "Lin", "score": "20", "hours": "2"},
            {"name": "Ada", "score": "", "hours": "3"},
        ])

        self.assertEqual(3, report["row_count"])
        self.assertEqual("number", report["columns"]["score"]["type"])
        self.assertEqual(1, report["columns"]["score"]["missing_count"])
        self.assertEqual(15.0, report["columns"]["score"]["mean"])
        self.assertEqual("string", report["columns"]["name"]["type"])
        self.assertEqual({"value": "Ada", "count": 2}, report["columns"]["name"]["top_values"][0])
        self.assertEqual(1.0, report["correlations"][0]["coefficient"])

    def test_rejects_non_tabular_json(self):
        with tempfile.TemporaryDirectory() as directory:
            input_path = Path(directory) / "records.json"
            input_path.write_text(json.dumps({"a": "x"}), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "array of objects"):
                data_analyzer.load_rows(input_path, "utf-8")


if __name__ == "__main__":
    unittest.main()
