import sys, os, unittest
from unittest.mock import patch
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
from tv_debloater import adb, debloat, protected, backup

class TestPhase2(unittest.TestCase):
    def test_protected_blocks(self):
        self.assertTrue(protected.is_protected("com.android.settings"))
    @patch("tv_debloater.adb._run", return_value="package:com.example\n")
    def test_dry_run_no_changes(self, m):
        res = debloat.execute("SER", ["com.example.app"], "disable", dry_run=True)
        self.assertTrue(res["dry_run"])
    def test_backup_roundtrip(self):
        bid,_ = backup.create_backup("SER", [{"package":"x","original_enabled":True,"action":"disable","timestamp":"t","result":None}])
        data = backup.load_backup(bid)
        self.assertEqual(data["device_serial"],"SER")

if __name__=="__main__":
    unittest.main()