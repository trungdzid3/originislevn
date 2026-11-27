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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.originisle.android.bluetooth.RedmiCaseColor

/**
 * Bottom sheet OriginUI ROM 16.0 cho phép tùy chỉnh giao diện thẻ popup, ảnh / GIF và preset
 * hiển thị cho từng thiết bị Bluetooth đã ghép đôi.
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
    var redmiCaseColor by remember(refreshVersion) {
        mutableStateOf(BluetoothPopupStore.getRedmiCaseColor(context))
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
                    .filter { dev ->
                        val name = runCatching { dev.name }.getOrNull().orEmpty()
                        BluetoothPopupStore.isHeadphoneDevice(dev, name)
                    }
                    .sortedBy { dev ->
                        runCatching { dev.name }.getOrNull().orEmpty().lowercase()
                    }
            }.getOrDefault(emptyList())
        }
    }

    val sheetShape = G2RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val isDark = IsleColors.isDark

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = sheetShape,
        containerColor = IsleColors.Surface,
        contentColor = IsleColors.Text,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (isDark) {
                            Modifier.border(
                                width = 0.6.dp,
                                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, Color.Transparent)),
                                shape = sheetShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(IsleColors.SurfaceHigher),
                )
            }
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = IsleColors.Text,
                )
                Text(
                    "Chỉ hỗ trợ tai nghe Bluetooth (đã lược bỏ loa, đồng hồ và thiết bị khác). Hỗ trợ model 3D động (Lenovo LE208, Redmi Buds 6 Play), ảnh tĩnh và GIF động cho thẻ popup kết nối nhanh.",
                    fontSize = 14.sp,
                    color = IsleColors.TextSub,
                    lineHeight = 19.sp,
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
                            variant = PillButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth(),
                            height = 50.dp,
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
                            "Chưa có tai nghe Bluetooth đã ghép đôi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Chỉ hỗ trợ tai nghe Bluetooth (đã lược bỏ loa, đồng hồ và thiết bị khác). Hãy ghép đôi tai nghe trong Cài đặt Bluetooth rồi quay lại tùy chỉnh.",
                            fontSize = 13.sp,
                            color = IsleColors.TextSub,
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Thẻ HeroCard (30.dp G2) preview thiết bị & bật/tắt popup
                        HeroCard(containerColor = IsleColors.SurfaceHigh) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                val customBitmap = remember(config.customImagePath) {
                                    config.customImagePath?.let { path ->
                                        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                                    }
                                }
                                val previewShape = G2RoundedCornerShape(18.dp)
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(previewShape)
                                        .background(IsleColors.Surface)
                                        .border(
                                            width = 0.8.dp,
                                            brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                                            shape = previewShape,
                                        ),
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
                                                .padding(6.dp),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = deviceName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
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
                                            fontWeight = if (hasCustom) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (hasCustom) IsleColors.GoodText else IsleColors.TextSub,
                                        )
                                    }
                                    if (!config.customImagePath.isNullOrBlank()) {
                                        Text(
                                            text = "Xóa ảnh tùy chỉnh",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IsleColors.BadText,
                                            modifier = Modifier
                                                .clip(G2RoundedCornerShape(8.dp))
                                                .clickable(role = Role.Button) {
                                                    BluetoothPopupStore.clearCustomImage(context, key)
                                                    refreshVersion++
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
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
                        }

                        // Thẻ GroupCard (24.dp G2) chứa tùy chọn preset 3D, màu vỏ & nút chọn ảnh/GIF/xem thử
                        GroupCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Model 3D & Preset hiển thị",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IsleColors.Text,
                                )

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

                                if (config.preset == BluetoothDevicePreset.REDMI_BUDS_6_PLAY) {
                                    GroupDivider(inset = 0.dp)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Màu vỏ hộp 3D",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = IsleColors.TextSub,
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            RedmiCaseColor.entries.forEach { caseColor ->
                                                val selected = redmiCaseColor == caseColor
                                                Row(
                                                    modifier = Modifier
                                                        .heightIn(min = 38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (selected) {
                                                                IsleColors.AccentSoft
                                                            } else {
                                                                IsleColors.SurfaceHigher
                                                            },
                                                        )
                                                        .border(
                                                            width = if (selected) 1.2.dp else 0.5.dp,
                                                            color = if (selected) IsleColors.Accent else IsleColors.BorderHairline,
                                                            shape = CircleShape,
                                                        )
                                                        .clickable(role = Role.RadioButton) {
                                                            redmiCaseColor = caseColor
                                                            BluetoothPopupStore.setRedmiCaseColor(context, caseColor)
                                                            refreshVersion++
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(caseColor.swatchHex))
                                                            .border(0.5.dp, IsleColors.BorderHairline, CircleShape),
                                                    )
                                                    Text(
                                                        text = caseColor.label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (selected) {
                                                            IsleColors.Accent
                                                        } else {
                                                            IsleColors.Text
                                                        },
                                                    )
                                                }
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
                                        variant = PillButtonVariant.ALPHA_GRAY,
                                        modifier = Modifier.weight(1f),
                                        height = 50.dp,
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
                                        variant = PillButtonVariant.PRIMARY,
                                        modifier = Modifier.weight(1f),
                                        height = 50.dp,
                                    )
                                }
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
    BluetoothDevicePreset.LENOVO_LE208 -> R.drawable.bt_preset_lenovo_le208_3d
    BluetoothDevicePreset.REDMI_BUDS_6_PLAY -> R.drawable.bt_preset_redmi_buds_6_play_3d
    BluetoothDevicePreset.TWS_EARBUDS -> R.drawable.bt_preset_tws_dark_3d
    BluetoothDevicePreset.TWS_WHITE -> R.drawable.bt_preset_tws_white_3d
    BluetoothDevicePreset.OVER_EAR -> R.drawable.bt_preset_over_ear_3d
}
