import datetime
from . import adb, protected

def is_enabled(serial, pkg):
    try:
        out = adb._run(serial, ["shell","pm","list","packages","-e"])
        return f"package:{pkg}" in out.splitlines()
    except Exception:
        return None

def plan(serial, pkgs, action):
    plans=[]
    for p in pkgs:
        plans.append({
            "package": p,
            "classification": "unclassified (location-based only)",
            "action": action,
            "risk": "HIGH" if protected.is_protected(p) else "review-required",
            "protected": protected.is_protected(p),
        })
    return plans

def execute(serial, pkgs, action, dry_run=True, confirm=False):
    plans = plan(serial, pkgs, action)
    for pl in plans:
        print(f"{pl['package']} | {pl['classification']} | {pl['action']} | risk={pl['risk']}")
    if any(p["protected"] for p in plans):
        print("ERROR: protected package selected. Aborting.")
        return {"aborted": True, "reason":"protected"}
    if dry_run:
        print("Dry-run: no changes made.")
        return {"dry_run": True, "plans": plans}
    if not confirm:
        print("Confirmation required. Re-run with --confirm.")
        return {"needs_confirm": True, "plans": plans}
    # backup first
    entries=[]
    for p in pkgs:
        entries.append({
            "package": p,
            "original_enabled": is_enabled(serial,p),
            "action": action,
            "timestamp": datetime.datetime.utcnow().isoformat()+"Z",
            "result": None,
        })
    from . import backup
    bid,_=backup.create_backup(serial, entries)
    print("Backup created:", bid)
    results=[]
    for e in entries:
        p=e["package"]
        try:
            if action == "disable":
                adb._run(serial, ["shell","pm","disable-user","--user","0",p])
            elif action == "uninstall-user0":
                adb._run(serial, ["shell","pm","uninstall","--user","0",p])
            e["result"]="success"
        except Exception as ex:
            e["result"]=f"error: {ex}"
        results.append(e)
    # update backup with results
    backup.create_backup(serial, results)
    print("Operations complete.")
    return {"backup_id": bid, "results": results}