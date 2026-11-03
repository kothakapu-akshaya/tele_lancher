from typing import List, Dict

SYSTEM_PREFIXES = ("/system/", "/vendor/", "/product/", "/odm/")

def classify_package(pkg: str, path: str) -> str:
    # Evidence-based only; no spyware/tracker claims.
    if path.startswith(SYSTEM_PREFIXES):
        return "system"
    if path.startswith("/data/"):
        return "user-installed"
    return "unknown"

def build_inventory(serial: str, packages: List[str], paths: Dict[str,str]) -> dict:
    system, user, oem, review = [], [], [], []
    for p in packages:
        path = paths.get(p, "")
        cat = classify_package(p, path)
        if cat == "system":
            # OEM/preinstalled heuristic only if vendor path
            if path.startswith(("/vendor/","/product/","/odm/")):
                oem.append(p)
            else:
                system.append(p)
        elif cat == "user-installed":
            user.append(p)
        else:
            review.append(p)
    return {
        "serial": serial,
        "system_packages": sorted(system),
        "user_installed_packages": sorted(user),
        "oem_preinstalled_packages": sorted(oem),
        "manual_review_packages": sorted(review),
        "counts": {
            "system": len(system),
            "user_installed": len(user),
            "oem_preinstalled": len(oem),
            "manual_review": len(review),
            "total": len(packages),
        },
        "note": "Classifications are install-location based only. No spyware/tracker/bloatware claims are made without evidence.",
    }