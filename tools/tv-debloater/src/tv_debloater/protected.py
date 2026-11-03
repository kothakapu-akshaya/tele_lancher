CRITICAL = {
 "com.android.settings","com.android.systemui","com.android.launcher","com.android.launcher3",
 "com.android.inputmethod.latin","com.android.providers.settings","com.android.networkstack",
 "com.android.permissioncontroller","com.android.packageinstaller","com.android.shell",
 "com.google.android.gms","com.google.android.gsf","com.android.providers.downloads",
 "com.telelauncher",
}
KEYWORDS = ("launcher","input","accessibility","network","systemui","settings","provider","service")

def is_protected(pkg: str) -> bool:
    if pkg in CRITICAL: return True
    low = pkg.lower()
    return any(k in low for k in KEYWORDS)