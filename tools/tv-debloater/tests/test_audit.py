import sys, os, unittest
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
from tv_debloater.classify import classify_package, build_inventory

class TestAudit(unittest.TestCase):
    def test_classify(self):
        self.assertEqual(classify_package("a","/system/app/a/a.apk"),"system")
        self.assertEqual(classify_package("b","/data/app/b/base.apk"),"user-installed")
    def test_inventory_counts(self):
        inv = build_inventory("s", ["x","y"], { "x":"/system/app/x/x.apk", "y":"/data/app/y/base.apk"})
        self.assertEqual(inv["counts"]["total"],2)

if __name__ == "__main__":
    unittest.main()
