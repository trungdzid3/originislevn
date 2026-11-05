# -*- coding: utf-8 -*-
import os, re

base_dir = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main"
str_regex = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')

ignore_strings = {
    "", " ", "NAVIGATION", "MEDIA", "AMOUNT", "TAXI", "DELIEVERY", "FLIGHT", "SPORTS",
    "com.autonavi.minimap", "oi_scene", "oi_template", "oi_right_template",
    "title", "text", "oi_left_content", "status_chip_text", "icon_res", "action_intent",
    "action_label", "oi_dismiss_intent", "oi_tag", "oi_id", "oi_sub_text", "oi_color",
    "cast_notifications", "cast_media_sessions", "cast_include_messages", "cast_ignore_silent",
    "cast_lockscreen_live_card", "cast_auto_dismiss_seconds", "cast_otp", "cast_show_actions",
    "cast_charging", "cast_charging_keep", "cast_privacy_dot", "cast_earbuds", "cast_ignored_apps",
    "cast_quiet_dnd", "cast_quiet_schedule", "cast_quiet_start", "cast_quiet_end",
    "onboarding_autostart_ack", "onboarding_done",
    "android.permission.POST_NOTIFICATIONS", "android.permission.BLUETOOTH_CONNECT",
    "HH:mm:ss", "yyyy-MM-dd"
}

results = {}

for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.kt'):
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                content = fp.read()
            matches = str_regex.findall(content)
            interesting = []
            for m in matches:
                if len(m) > 1 and not m.startswith("android.") and not m.startswith("com.") and not m.startswith("http") and not m.startswith("ic_") and m not in ignore_strings:
                    # check if contains ASCII letters and looks like English
                    if re.search(r'[a-zA-Z]{3,}', m) and not re.search(r'[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]', m):
                        interesting.append(m)
            if interesting:
                results[os.path.relpath(p, base_dir)] = interesting

out_path = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\scan_results.txt"
with open(out_path, "w", encoding="utf-8") as out:
    for rel_path, strings in sorted(results.items()):
        out.write(f"=== {rel_path} ===\n")
        for s in set(strings):
            out.write(f"  {s}\n")
        out.write("\n")

print(f"Scanned {len(results)} files with remaining English strings.")
