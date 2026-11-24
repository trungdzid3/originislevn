package com.originisle.android.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.bluetooth.BluetoothDevicePreset
import com.originisle.android.bluetooth.BluetoothPopupBattery
import com.originisle.android.bluetooth.BluetoothPopupOverlay
import com.originisle.android.bluetooth.BluetoothPopupStore
import com.originisle.android.bluetooth.BluetoothPopupTheme

/**
 * Bottom sheet Material 3 cho phép tùy chỉnh giao diện thẻ popup, ảnh / GIF và preset hiển thị
 * cho từng thiết bị Bluetooth đã ghép đôi.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BluetoothPopupSheet(
    context: Context,
    onDismiss: () -> Unit,
) {
    val tick = rememberResumeTick()
    var refreshVersion by remember { mutableIntStateOf(0) }
    var pickingForKey by remember { mutableStateOf<String?>(null) }
    var popupTheme by remember(refreshVersion) {
        mutableStateOf(BluetoothPopupStore.getPopupTheme(context))
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        tick.intValue++
        if (!granted) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val targetKey = pickingForKey
        pickingForKey = null
        if (uri != null && targetKey != null) {
            val savedPath = BluetoothPopupStore.saveCustomImage(context, targetKey, uri)
            refreshVersion++
            Toast.makeText(
                context,
                if (savedPath != null) "Đã lưu ảnh tùy chỉnh" else "Không thể đọc ảnh đã chọn",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val hasBtPermission = remember(tick.intValue) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    val bondedDevices: List<BluetoothDevice> = remember(hasBtPermission, tick.intValue) {
        if (!hasBtPermission) {
            emptyList()
        } else {
            runCatching {
                val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bm?.adapter?.bondedDevices.orEmpty()
                    .sortedBy { dev ->
                        runCatching { dev.name }.getOrNull().orEmpty().lowercase()
                    }
            }.getOrDefault(emptyList())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IsleColors.Background,
        contentColor = IsleColors.Text,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(IsleColors.SurfaceHigher),
            )
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Ảnh thiết bị Bluetooth",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = IsleColors.Text,
                )
                Text(
                    "Chọn ảnh tĩnh, ảnh GIF hoặc mẫu thiết bị cho thẻ popup kết nối nhanh.",
                    fontSize = 13.sp,
                    color = IsleColors.TextSecondary,
                    lineHeight = 18.sp,
                )
            }

            GroupCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Giao diện thẻ Popup",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsleColors.Text,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BluetoothPopupTheme.entries.forEach { theme ->
                            ChoiceChip(
                                label = theme.label,
                                selected = popupTheme == theme,
                                onClick = {
                                    popupTheme = theme
                                    BluetoothPopupStore.setPopupTheme(context, theme)
                                },
                                height = 38.dp,
                                horizontalPadding = 12.dp,
                            )
                        }
                    }
                }
            }

            if (!hasBtPermission) {
                GroupCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Cần quyền Thiết bị lân cận (Bluetooth) để đọc danh sách tai nghe đã ghép đôi.",
                            fontSize = 14.sp,
                            color = IsleColors.Text,
                            lineHeight = 20.sp,
                        )
                        PillButton(
                            text = "Cấp quyền Bluetooth",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 46.dp,
                        )
                    }
                }
            } else if (bondedDevices.isEmpty()) {
                GroupCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Chưa có thiết bị Bluetooth đã ghép đôi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Hãy ghép đôi tai nghe, loa hoặc đồng hồ trong Cài đặt Bluetooth rồi quay lại tùy chỉnh.",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                            lineHeight = 18.sp,
                        )
                    }
                }
            } else {
                bondedDevices.forEach { device ->
                    val address = runCatching { device.address }.getOrNull().orEmpty()
                    val deviceName = runCatching { device.name }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: address.ifBlank { "Thiết bị Bluetooth" }
                    val key = address.ifBlank { deviceName }
                    val shortMac = if (address.length >= 8) address.takeLast(8) else address
                    val config = remember(key, deviceName, refreshVersion) {
                        BluetoothPopupStore.getDeviceConfig(context, key, deviceName)
                    }

                    GroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val customBitmap = remember(config.customImagePath) {
                                    config.customImagePath?.let { path ->
                                        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .border(0.5.dp, IsleColors.BorderHairline, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (customBitmap != null) {
                                        Image(
                                            bitmap = customBitmap,
                                            contentDescription = deviceName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(presetDrawableRes(config.preset)),
                                            contentDescription = config.preset.label,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = deviceName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IsleColors.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (shortMac.isNotBlank()) {
                                            Text(
                                                text = shortMac,
                                                fontSize = 12.sp,
                                                color = IsleColors.TextTertiary,
                                            )
                                        }
                                        val hasCustom = !config.customImagePath.isNullOrBlank()
                                        Text(
                                            text = if (hasCustom) "Ảnh tùy chỉnh" else config.preset.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (hasCustom) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (hasCustom) IsleColors.Good else IsleColors.TextSecondary,
                                        )
                                        if (hasCustom) {
                                            Text(
                                                text = "Xóa ảnh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = IsleColors.Bad,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable(role = Role.Button) {
                                                        BluetoothPopupStore.clearCustomImage(context, key)
                                                        refreshVersion++
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                IsleSwitch(
                                    checked = config.enabled,
                                    onCheckedChange = { enabled ->
                                        BluetoothPopupStore.setDeviceEnabled(context, key, enabled)
                                        refreshVersion++
                                    },
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val presets = BluetoothDevicePreset.entries
                                val rows = presets.chunked(2)
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowItems.forEach { preset ->
                                            ChoiceChip(
                                                label = preset.label,
                                                selected = config.preset == preset,
                                                onClick = {
                                                    BluetoothPopupStore.setDevicePreset(context, key, preset)
                                                    refreshVersion++
                                                },
                                                modifier = Modifier.weight(1f),
                                                height = 38.dp,
                                                horizontalPadding = 8.dp,
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                PillButton(
                                    text = "Chọn ảnh / GIF",
                                    onClick = {
                                        pickingForKey = key
                                        imagePicker.launch("image/*")
                                    },
                                    modifier = Modifier.weight(1f),
                                    container = MaterialTheme.colorScheme.secondaryContainer,
                                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                                    height = 42.dp,
                                )
                                PillButton(
                                    text = "Xem thử",
                                    onClick = {
                                        if (!isOverlayPermissionGranted(context)) {
                                            openOverlayPermissionSettings(context)
                                        } else {
                                            BluetoothPopupOverlay.showOrUpdate(
                                                context,
                                                key,
                                                deviceName,
                                                BluetoothPopupBattery(
                                                    main = 85,
                                                    left = 90,
                                                    right = 85,
                                                    caseBattery = 70,
                                                ),
                                                forcePreview = true,
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    height = 42.dp,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@DrawableRes
private fun presetDrawableRes(preset: BluetoothDevicePreset): Int = when (preset) {
    BluetoothDevicePreset.TWS_EARBUDS -> R.drawable.bt_preset_tws_dark_3d
    BluetoothDevicePreset.TWS_WHITE -> R.drawable.bt_preset_tws_white_3d
    BluetoothDevicePreset.OVER_EAR -> R.drawable.bt_preset_over_ear_3d
    BluetoothDevicePreset.SPEAKER -> R.drawable.bt_preset_speaker_3d
    BluetoothDevicePreset.WATCH -> R.drawable.bt_preset_watch_3d
}
