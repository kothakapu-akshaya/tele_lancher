import argparse, sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "src"))
from tv_debloater import adb, classify, export, debloat, backup, restore, benchmark

def cmd_devices(args):
    for d in adb.list_devices():
        print(f"{d.serial}\t{d.state}")

def cmd_audit(args):
    serial = args.serial
    devs = {d.serial: d for d in adb.list_devices()}
    if serial not in devs or devs[serial].state != "device":
        print("Device not available.")
        return 2
    info = adb.device_info(serial)
    print("Device:", info)
    pkgs = adb.list_packages(serial)
    paths = adb.package_paths(serial)
    inv = classify.build_inventory(serial, pkgs, paths)
    inv["device_info"] = info
    if args.format == "json":
        export.to_json(inv, args.output or "inventory.json"); print("Wrote", args.output or "inventory.json")
    elif args.format == "csv":
        export.to_csv(inv, args.output or "inventory.csv"); print("Wrote", args.output or "inventory.csv")
    else:
        print("Counts:", inv["counts"])
    return 0

def cmd_debloat(args):
    serial=args.serial
    devs={d.serial:d for d in adb.list_devices()}
    if serial not in devs or devs[serial].state!="device":
        print("Device not available."); return 2
    pkgs=[args.package] if args.package else []
    if not pkgs:
        print("Explicit --package required."); return 2
    debloat.execute(serial, pkgs, args.action, dry_run=not args.apply, confirm=args.confirm)
    return 0

def cmd_backups(args):
    for b in backup.list_backups():
        print(b["backup_id"], b["device_serial"], b["created_at"], len(b["entries"]))
    return 0

def cmd_restore(args):
    restore.restore(args.backup)
    return 0

def cmd_benchmark(args):
    serial=args.serial
    devs={d.serial:d for d in adb.list_devices()}
    if serial not in devs or devs[serial].state!="device":
        print("Device not available."); return 2
    benchmark.collect(serial, args.label)
    return 0

def cmd_benchmark_report(args):
    benchmark.report()
    return 0

def main():
    ap=argparse.ArgumentParser()
    sub=ap.add_subparsers(dest="cmd", required=True)
    d=sub.add_parser("devices"); d.set_defaults(func=cmd_devices)
    a=sub.add_parser("audit")
    a.add_argument("--serial", required=True)
    a.add_argument("--format", choices=["text","json","csv"], default="text")
    a.add_argument("--output"); a.set_defaults(func=cmd_audit)
    db=sub.add_parser("debloat")
    db.add_argument("--serial", required=True)
    db.add_argument("--package")
    db.add_argument("--action", choices=["disable","uninstall-user0"], default="disable")
    db.add_argument("--apply", action="store_true")
    db.add_argument("--confirm", action="store_true")
    db.set_defaults(func=cmd_debloat)
    b=sub.add_parser("backups"); b.set_defaults(func=cmd_backups)
    r=sub.add_parser("restore")
    r.add_argument("--backup", required=True); r.set_defaults(func=cmd_restore)
    bm=sub.add_parser("benchmark")
    bm.add_argument("--serial", required=True)
    bm.add_argument("--label", choices=["before","after"], required=True)
    bm.set_defaults(func=cmd_benchmark)
    br=sub.add_parser("benchmark-report"); br.set_defaults(func=cmd_benchmark_report)
    args=ap.parse_args()
    return args.func(args) or 0

if __name__=="__main__":
    raise SystemExit(main())