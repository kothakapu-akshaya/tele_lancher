import csv, json

def to_json(data, path):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)

def to_csv(data, path):
    rows = []
    for key in ["system_packages","user_installed_packages","oem_preinstalled_packages","manual_review_packages"]:
        for pkg in data.get(key, []):
            rows.append({"category": key, "package": pkg})
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["category","package"])
        w.writeheader()
        w.writerows(rows)