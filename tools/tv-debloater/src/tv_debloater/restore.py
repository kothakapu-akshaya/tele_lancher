from . import adb, backup

def restore(bid):
    data = backup.load_backup(bid)
    serial = data["device_serial"]
    serials = [d.serial for d in adb.list_devices()]
    if serial not in serials:
        print("Original device not connected.")
        return
    for e in data["entries"]:
        pkg = e["package"]
        action = e.get("action")
        try:
            if action == "disable":
                adb._run(serial, ["shell","pm","enable",pkg])
                print("Restored (enabled):", pkg)
            elif action == "uninstall-user0":
                adb._run(serial, ["shell","cmd","package","install-existing",pkg])
                print("Restored (install-existing):", pkg)
            else:
                print("Unknown action, cannot restore:", pkg)
        except Exception as ex:
            print("Cannot restore automatically:", pkg, ex)