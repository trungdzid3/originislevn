# -*- coding: utf-8 -*-
path = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\java\com\originisle\android\service\NotificationCastListener.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Update SYSTEM_PKGS check
old_system = 'if (sbn.packageName in SYSTEM_PKGS) return'
new_system = 'if (sbn.packageName in SYSTEM_PKGS && !DownloadDetector.isDownload(sbn)) return'
text = text.replace(old_system, new_system)

# 2. Update QuietHours check to allow download
old_quiet = '!isCall && !isNavigation(sbn, isOngoingNow) && !isTimer(sbn)'
new_quiet = '!isCall && !isNavigation(sbn, isOngoingNow) && !isTimer(sbn) && !DownloadDetector.isDownload(sbn)'
text = text.replace(old_quiet, new_quiet)

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Updated NotificationCastListener.kt successfully")
