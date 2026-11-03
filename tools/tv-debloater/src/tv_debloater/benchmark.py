import datetime, json, os, subprocess
from . import adb

BENCH_DIR = os.path.expanduser("~/.tv-debloater/benchmarks")

def ensure_dir(): os.makedirs(BENCH_DIR, exist_ok=True)

def _run(serial, args):
    return adb._run(serial, args)

def collect(serial, label):
    ensure_dir()
    info = adb.device_info(serial)
    # RAM
    meminfo = _run(serial, ["shell","cat","/proc/meminfo"])
    total_kb = avail_kb = None
    for line in meminfo.splitlines():
        if line.startswith("MemTotal:"):
            total_kb = int(line.split()[1])
        if line.startswith("MemAvailable:"):
            avail_kb = int(line.split()[1])
    used_kb = (total_kb - avail_kb) if (total_kb and avail_kb) else None
    # CPU
    cpu = None
    try:
        cpu = _run(serial, ["shell","cat","/proc/cpuinfo"]).splitlines()[0:5]
    except Exception:
        cpu = None
    # running processes count
    ps = _run(serial, ["shell","ps","-A"])
    proc_count = len([l for l in ps.splitlines() if l.strip()]) - 1
    # cold/warm start Tele Launcher (may fail)
    cold = warm = None
    try:
        # force stop then start with am start -W
        _run(serial, ["shell","am","force-stop","com.telelauncher"])
        out = _run(serial, ["shell","am","start","-W","-n","com.telelauncher/.MainActivity"])
        for line in out.splitlines():
            if "TotalTime" in line:
                cold = int(line.split(":")[-1].strip().replace("ms",""))
    except Exception:
        cold = None
    # warm start
    try:
        out = _run(serial, ["shell","am","start","-W","-n","com.telelauncher/.MainActivity"])
        for line in out.splitlines():
            if "TotalTime" in line:
                warm = int(line.split(":")[-1].strip().replace("ms",""))
    except Exception:
        warm = None
    # boot time unreliable
    boot = None
    try:
        bt = _run(serial, ["shell","getprop","sys.boot_completed"])
        boot = bt.strip()
    except Exception:
        boot = None

    data = {
        "label": label,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "device": info,
        "ram_total_kb": total_kb,
        "ram_available_kb": avail_kb,
        "ram_used_kb": used_kb,
        "cpu_info": cpu,
        "running_process_count": proc_count,
        "telelauncher_cold_start_ms": cold,
        "telelauncher_warm_start_ms": warm,
        "boot_completed_flag": boot,
        "limitations": "Boot time not reliably measured; cold/warm start may be null if am start unavailable. Metrics are best-effort.",
    }
    path = os.path.join(BENCH_DIR, f"{label}_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(path,"w") as f:
        json.dump(data, f, indent=2)
    print("Saved", path)
    return data

def pct_change(before, after):
    if before is None or after is None or before == 0:
        return None
    return round((after-before)/before*100, 2)

def report():
    ensure_dir()
    files = sorted(os.listdir(BENCH_DIR))
    before = after = None
    for fn in files:
        if not fn.endswith(".json"): continue
        with open(os.path.join(BENCH_DIR,fn)) as f:
            d=json.load(f)
        if d["label"]=="before": before=d
        if d["label"]=="after": after=d
    if not before or not after:
        print("Need both before and after benchmarks.")
        return
    lines=[]
    lines.append("# Benchmark Report")
    lines.append("")
    lines.append(f"Before: {before['timestamp']}")
    lines.append(f"After: {after['timestamp']}")
    lines.append("")
    lines.append("| Metric | Before | After | % Change |")
    lines.append("|---|---|---|---|")
    metrics = [
        ("RAM used (KB)", "ram_used_kb"),
        ("RAM available (KB)", "ram_available_kb"),
        ("Process count", "running_process_count"),
        ("Tele cold start (ms)", "telelauncher_cold_start_ms"),
        ("Tele warm start (ms)", "telelauncher_warm_start_ms"),
    ]
    for name,key in metrics:
        b=before.get(key); a=after.get(key)
        pc=pct_change(b,a)
        lines.append(f"| {name} | {b} | {a} | {pc if pc is not None else 'n/a'} |")
    lines.append("")
    lines.append("## Limitations")
    lines.append("- Boot time not reliably measured.")
    lines.append("- Cold/warm start may be unavailable.")
    lines.append("- Results must support any performance claim; null metrics are not estimated.")
    md="\n".join(lines)
    out=os.path.join(BENCH_DIR,"report.md")
    with open(out,"w") as f:
        f.write(md)
    print(md)
    print("Wrote", out)