import xml.etree.ElementTree as ET, os, sys
base="app/src/main/res"
langs=["","-hi","-te"]
def keys(lang):
    p=os.path.join(base, f"values{lang}", "strings.xml")
    t=ET.parse(p)
    return {e.get("name") for e in t.getroot() if e.tag=="string"}
en=keys("")
for l in ["-hi","-te"]:
    k=keys(l)
    missing=en-k
    extra=k-en
    print(l, "missing:", sorted(missing), "extra:", sorted(extra))
    if missing: sys.exit(1)
print("parity ok")