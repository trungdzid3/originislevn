package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Per-app allow list: every launchable app, grouped A–Z, each with a switch. */
@Composable
fun AppsScreen(context: Context, prefs: SharedPreferences, onBack: () -> Unit) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    // 0 = all, 1 = on only, 2 = off only.
    var filter by remember { mutableIntStateOf(0) }
    var showSystem by remember { mutableStateOf(prefs.getBoolean("apps_show_system", false)) }
    val ignored = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty()) }
    }
    // Icons are decoded per visible row (hundreds with system apps on), kept here while scrolling.
    val iconCache = remember { HashMap<String, ImageBitmap?>() }
    val appsCache = remember { HashMap<Boolean, List<AppEntry>>() }

    LaunchedEffect(showSystem) {
        val cached = appsCache[showSystem]
        if (cached != null) {
            apps = cached
            loading = false
        } else {
            loading = true
            val loaded = withContext(Dispatchers.IO) { loadApps(context, showSystem, ignored.toSet()) }
            appsCache[showSystem] = loaded
            apps = loaded
            loading = false
        }
    }

    val filtered by remember {
        derivedStateOf {
            apps.filter { app ->
                (query.isBlank() || app.label.contains(query, ignoreCase = true)) &&
                    when (filter) {
                        1 -> app.pkg !in ignored
                        2 -> app.pkg in ignored
                        else -> true
                    }
            }
        }
    }
    val groups by remember {
        derivedStateOf {
            filtered.groupBy { it.label.firstOrNull()?.takeIf(Char::isLetter)?.uppercaseChar() ?: '#' }
        }
    }
    val anyOff = filtered.any { it.pkg in ignored }

    OriginCollapsingScaffold(
        title = "Ứng dụng",
        onBack = onBack,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        trailing = {
            if (!loading && filtered.isNotEmpty()) {
                PillButton(
                    text = if (anyOff) "Bật tất cả" else "Tắt tất cả",
                    onClick = {
                        if (anyOff) ignored.removeAll(filtered.map { it.pkg }.toSet())
                        else filtered.forEach { if (it.pkg !in ignored) ignored.add(it.pkg) }
                        prefs.edit().putStringSet("cast_ignored_apps", ignored.toSet()).apply()
                    },
                    variant = PillButtonVariant.ALPHA_BLUE,
                    height = 38.dp,
                )
            }
        },
    ) {
        item(key = "apps_controls") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            ) {
                VSearchView2(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Tìm kiếm ứng dụng",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Tất cả", "Đang bật", "Đã tắt").forEachIndexed { index, label ->
                        ChoiceChip(
                            label = label,
                            selected = filter == index,
                            onClick = { filter = index },
                            selectedContainer = IsleColors.AccentSoft,
                            selectedContent = IsleColors.Accent,
                            shape = G2RoundedCornerShape(18.dp),
                            height = 38.dp,
                            horizontalPadding = 14.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Grid,
                        tileColor = IsleColors.TileGray,
                        title = "Ứng dụng hệ thống",
                        checked = showSystem,
                        subtitle = "Hiển thị cả các ứng dụng và dịch vụ không có biểu tượng",
                    ) {
                        showSystem = it
                        prefs.edit().putBoolean("apps_show_system", it).apply()
                    }
                }

                Text(
                    text = "Tắt những ứng dụng bạn không muốn hiển thị trên đảo. Tin nhắn văn bản thông thường đã được lọc tự động, nên cuộc gọi vẫn sẽ hiển thị ngay cả khi tắt tin nhắn.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IsleColors.TextSub,
                )
            }
        }

        if (loading) {
            item(key = "apps_loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IsleColors.Accent)
                }
            }
        } else if (filtered.isEmpty()) {
            item(key = "apps_empty") {
                GroupCard {
                    EmptyMessage(
                        if (query.isNotBlank()) "Không có ứng dụng nào khớp với \"$query\"." else "Không có ứng dụng nào",
                    )
                }
            }
        }

        if (!loading) groups.forEach { (letter, items) ->
            item(key = "letter-$letter") {
                Text(
                    text = letter.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = IsleColors.TextSub,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
                )
            }
            itemsIndexed(items, key = { _, app -> app.pkg }) { i, app ->
                val on = app.pkg !in ignored
                val shape = groupItemShape(i, items.size)
                val isDark = IsleColors.isDark
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(IsleColors.Surface)
                        .then(
                            if (isDark) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                                    shape = shape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (i > 0) GroupDivider(inset = 66.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = on, role = Role.Switch) { allowed ->
                                if (allowed) ignored.remove(app.pkg) else if (app.pkg !in ignored) ignored.add(app.pkg)
                                prefs.edit().putStringSet("cast_ignored_apps", ignored.toSet()).apply()
                            }
                            .heightIn(min = 60.dp)
                            .padding(start = 16.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(36.dp).clip(G2RoundedCornerShape(11.dp))) {
                            AppIcon(context, app, iconCache)
                        }
                        Text(
                            text = app.label,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) IsleColors.Text else IsleColors.TextSub,
                        )
                        IsleSwitch(on, onCheckedChange = null)
                    }
                }
            }
        }
    }
}

private data class AppEntry(val pkg: String, val label: String, val info: ApplicationInfo)

@Composable
private fun AppIcon(context: Context, app: AppEntry, cache: HashMap<String, ImageBitmap?>) {
    val icon by produceState(cache[app.pkg], app.pkg) {
        if (app.pkg !in cache) {
            cache[app.pkg] = withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getApplicationIcon(app.info).toBitmap(72, 72).asImageBitmap() }.getOrNull()
            }
        }
        value = cache[app.pkg]
    }
    icon?.let { Image(it, null, Modifier.size(36.dp)) }
}

private fun loadApps(context: Context, includeSystem: Boolean, alsoInclude: Set<String>): List<AppEntry> {
    val pm = context.packageManager
    val launchable = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_INFO).flatMapTo(HashSet()) { category ->
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).map { it.activityInfo.packageName }
    }
    return pm.getInstalledApplications(0)
        .mapNotNull { info ->
            val pkg = info.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val shown = pkg in launchable || pkg in alsoInclude
            if (!shown && !(includeSystem && info.enabled && !info.isResourceOverlay)) return@mapNotNull null
            val label = pm.getApplicationLabel(info).toString()
            if (!shown && label == pkg) return@mapNotNull null
            AppEntry(pkg = pkg, label = label, info = info)
        }
        .sortedBy { it.label.lowercase() }
}
