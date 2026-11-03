# TV ADB Debloater Toolkit (Phase 1-3)

Audit-only + safe reversible debloat + benchmarking for Android/Google TV. Does not modify the Tele Launcher Android app.

## Overview
External Python CLI using adb. No destructive system partition changes. Dry-run default.

## Architecture
- adb.py: subprocess wrapper, device info, packages.
- classify.py: install-location classification only.
- protected.py: critical package guard.
- debloat.py: planning, dry-run, guarded apply, backup.
- backup.py: versioned JSON backups.
- restore.py: best-effort restore.
- benchmark.py: metrics + Markdown report.
- main.py: CLI.

## Installation
```
cd tools/tv-debloater
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

## ADB Setup
Install Android Platform Tools, enable USB debugging on TV, authorize host.

## Device Connection
```
python3 main.py devices
```

## Package Audit
```
python3 main.py audit --serial DEVICE_SERIAL
python3 main.py audit --serial DEVICE_SERIAL --format json --output inventory.json
python3 main.py audit --serial DEVICE_SERIAL --format csv --output inventory.csv
```

## Classification
System/user/OEM/review by install path only. No spyware/tracker/bloat claims without evidence.

## Dry-Run Debloat
```
python3 main.py debloat --serial DEVICE_SERIAL --package com.example.app --dry-run
```

## Safe Package Modification
```
python3 main.py debloat --serial DEVICE_SERIAL --package com.example.app --action disable --apply --confirm
```
Uninstall-for-user-0 only explicitly:
```
python3 main.py debloat --serial DEVICE_SERIAL --package com.example.app --action uninstall-user0 --apply --confirm
```

## Backup and Rollback
```
python3 main.py backups
python3 main.py restore --backup BACKUP_ID
```
Restore is best-effort; cannot guarantee factory restoration.

## Benchmarking
```
python3 main.py benchmark --serial DEVICE_SERIAL --label before
python3 main.py benchmark --serial DEVICE_SERIAL --label after
python3 main.py benchmark-report
```

## Reports
JSON raw measurements; Markdown comparison report in ~/.tv-debloater/benchmarks/report.md.

## Safety Warnings
- Critical packages protected.
- Dry-run default.
- Explicit package selection required.
- No system partition removal.

## Known Limitations
- Metrics may be unavailable; not estimated.
- Cold/warm start may fail on some devices.
- Boot time not reliably measured.
- Restore may not recover data.

## Demonstration Sequence
1. Stock device
2. Package audit
3. Baseline benchmark
4. Debloat dry run
5. Explicitly approved package operation
6. Rollback
7. Post-operation benchmark
8. Compare results

## Tests
```
python3 -m unittest discover -s tests -v
```