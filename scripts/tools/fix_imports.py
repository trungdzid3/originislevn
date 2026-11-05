# -*- coding: utf-8 -*-
import os

BASE_DIR = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\java\com\originisle\android\ui"

# Ensure import androidx.compose.foundation.border in Components.kt
comp_path = os.path.join(BASE_DIR, "Components.kt")
with open(comp_path, "r", encoding="utf-8") as f:
    text = f.read()

if "import androidx.compose.foundation.border" not in text:
    text = text.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border")

text = text.replace("private val GroupRadius = 24.dp", "private val GroupRadius = 28.dp")

# Replace GroupCard definition cleanly
old_gc = """@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(GroupRadius)).background(IsleColors.Surface),
        content = content,
    )
}"""

new_gc = """@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupRadius))
            .background(IsleColors.Surface)
            .border(width = 0.8.dp, color = IsleColors.BorderHairline, shape = RoundedCornerShape(GroupRadius)),
        content = content,
    )
}"""

if old_gc in text:
    text = text.replace(old_gc, new_gc)

with open(comp_path, "w", encoding="utf-8") as f:
    f.write(text)
print("Updated Components.kt clean imports")

# Ensure import in HomeScreen.kt
home_path = os.path.join(BASE_DIR, "HomeScreen.kt")
with open(home_path, "r", encoding="utf-8") as f:
    text = f.read()

if "import androidx.compose.foundation.border" not in text:
    text = text.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border")

with open(home_path, "w", encoding="utf-8") as f:
    f.write(text)
print("Updated HomeScreen.kt clean imports")
