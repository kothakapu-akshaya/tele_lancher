import subprocess
from dataclasses import dataclass
from typing import List, Optional

@dataclass
class Device:
    serial: str
    state: str  # device/offline/unauthorized

def list_devices() -> List[Device]:
    out = subprocess.run(["adb", "devices"], capture_output=True, text=True, check=False)
    devices = []
    for line in out.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2:
            devices.append(Device(parts[0], parts[1]))
    return devices

def _run(serial: str, args: List[str]) -> str:
    cmd = ["adb", "-s", serial] + args
    r = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.strip() or "adb command failed")
    return r.stdout

def get_prop(serial: str, prop: str) -> str:
    return _run(serial, ["shell", "getprop", prop]).strip()

def device_info(serial: str) -> dict:
    return {
        "serial": serial,
        "manufacturer": get_prop(serial, "ro.product.manufacturer"),
        "model": get_prop(serial, "ro.product.model"),
        "android_version": get_prop(serial, "ro.build.version.release"),
        "sdk_version": get_prop(serial, "ro.build.version.sdk"),
    }

def list_packages(serial: str) -> List[str]:
    out = _run(serial, ["shell", "pm", "list", "packages"])
    pkgs = []
    for line in out.splitlines():
        line = line.strip()
        if line.startswith("package:"):
            pkgs.append(line[len("package:"):])
    return pkgs

def package_paths(serial: str) -> dict:
    # optional: identify system/user install location
    out = _run(serial, ["shell", "pm", "list", "packages", "-f"])
    result = {}
    for line in out.splitlines():
        line=line.strip()
        if line.startswith("package:"):
            path_name = line[len("package:"):].split("=",1)
            if len(path_name)==2:
                result[path_name[1]] = path_name[0]
    return result