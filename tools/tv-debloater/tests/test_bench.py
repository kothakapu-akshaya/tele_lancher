import sys, os, unittest, json, tempfile
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
from tv_debloater import benchmark

class TestBench(unittest.TestCase):
    def test_pct_change(self):
        self.assertEqual(benchmark.pct_change(100,80), -20.0)
        self.assertIsNone(benchmark.pct_change(0,10))
    def test_report_missing(self):
        # empty dir
        d=tempfile.mkdtemp()
        benchmark.BENCH_DIR=d
        # should print message not crash
        try:
            benchmark.report()
        except SystemExit:
            pass

if __name__=="__main__":
    unittest.main()