import json, os, datetime, uuid

BACKUP_DIR = os.path.expanduser("~/.tv-debloater/backups")

def ensure_dir():
    os.makedirs(BACKUP_DIR, exist_ok=True)

def create_backup(serial, entries):
    ensure_dir()
    bid = str(uuid.uuid4())[:8]
    path = os.path.join(BACKUP_DIR, f"{bid}.json")
    data = {
        "backup_id": bid,
        "device_serial": serial,
        "created_at": datetime.datetime.utcnow().isoformat()+"Z",
        "entries": entries,
    }
    with open(path,"w") as f:
        json.dump(data,f,indent=2)
    return bid, path

def list_backups():
    ensure_dir()
    out=[]
    for fn in sorted(os.listdir(BACKUP_DIR)):
        if fn.endswith(".json"):
            with open(os.path.join(BACKUP_DIR,fn)) as f:
                d=json.load(f)
            out.append(d)
    return out

def load_backup(bid):
    path=os.path.join(BACKUP_DIR, f"{bid}.json")
    with open(path) as f:
        return json.load(f)