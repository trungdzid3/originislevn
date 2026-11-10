package com.originisle.android.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Icon
import com.originisle.android.R

/**
 * Trình dựng đồ họa viên pin và biểu tượng thiết bị ngoại vi dùng Canvas cho Origin Island.
 */
object BatteryCanvasRenderer {

    /**
     * Dựng Icon viên pin nằm ngang tùy biến màu sắc theo dung lượng và hiển thị tia sét khi đang sạc.
     */
    fun createBatteryIcon(
        context: Context,
        level: Int,
        isCharging: Boolean,
        width: Int = 96,
        height: Int = 96
    ): Icon {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val w = width.toFloat()
        val h = height.toFloat()

        val bodyWidth = w * 0.62f
        val bodyHeight = h * 0.36f
        val capWidth = w * 0.05f
        val capHeight = bodyHeight * 0.44f
        val strokeWidth = maxOf(2f, w * 0.04f)
        val cornerRadius = maxOf(3f, bodyHeight * 0.22f)

        val totalWidth = bodyWidth + strokeWidth * 0.5f + capWidth
        val startX = (w - totalWidth) / 2f
        val startY = (h - bodyHeight) / 2f

        // 1. Viền thân pin
        val borderRect = RectF(
            startX + strokeWidth / 2f,
            startY + strokeWidth / 2f,
            startX + bodyWidth - strokeWidth / 2f,
            startY + bodyHeight - strokeWidth / 2f
        )
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = Color.parseColor("#D0FFFFFF")
        }
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)

        // 2. Cực dương bên phải
        val capLeft = startX + bodyWidth + strokeWidth * 0.2f
        val capTop = startY + (bodyHeight - capHeight) / 2f
        val capRect = RectF(capLeft, capTop, capLeft + capWidth, capTop + capHeight)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#D0FFFFFF")
        }
        val capRadius = maxOf(1.5f, capWidth * 0.35f)
        canvas.drawRoundRect(capRect, capRadius, capRadius, capPaint)

        // 3. Tô ruột viên pin (filled bar)
        val innerPadding = strokeWidth + maxOf(1.5f, w * 0.015f)
        val innerLeft = startX + innerPadding
        val innerTop = startY + innerPadding
        val innerRight = startX + bodyWidth - innerPadding
        val innerBottom = startY + bodyHeight - innerPadding
        val maxFillWidth = maxOf(0f, innerRight - innerLeft)
        val fillRatio = level.coerceIn(0, 100) / 100f
        val currentFillWidth = maxFillWidth * fillRatio

        if (currentFillWidth > 0f) {
            val fillRect = RectF(innerLeft, innerTop, innerLeft + currentFillWidth, innerBottom)
            val fillColor = when {
                level > 20 -> Color.parseColor("#00D084")
                level in 11..20 -> Color.parseColor("#FFB020")
                else -> Color.parseColor("#FF5247")
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            val innerRadius = maxOf(1f, cornerRadius - innerPadding * 0.5f)
            canvas.drawRoundRect(fillRect, innerRadius, innerRadius, fillPaint)
        }

        // 4. Biểu tượng tia sét khi sạc
        if (isCharging) {
            val cx = startX + bodyWidth / 2f
            val cy = startY + bodyHeight / 2f
            val boltW = bodyWidth * 0.24f
            val boltH = bodyHeight * 0.76f

            val boltPath = Path().apply {
                moveTo(cx + boltW * 0.1f, cy - boltH * 0.5f)
                lineTo(cx - boltW * 0.5f, cy + boltH * 0.05f)
                lineTo(cx - boltW * 0.05f, cy + boltH * 0.05f)
                lineTo(cx - boltW * 0.15f, cy + boltH * 0.5f)
                lineTo(cx + boltW * 0.5f, cy - boltH * 0.05f)
                lineTo(cx + boltW * 0.05f, cy - boltH * 0.05f)
                close()
            }

            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = maxOf(1.5f, strokeWidth * 0.5f)
                color = Color.parseColor("#60000000")
            }
            canvas.drawPath(boltPath, shadowPaint)

            val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
            canvas.drawPath(boltPath, boltPaint)
        }

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon an toàn từ drawable resource qua bitmap canvas.
     */
    fun createDeviceIcon(context: Context, glyphRes: Int): Icon {
        return runCatching {
            val drawable = context.getDrawable(glyphRes)
                ?: return Icon.createWithResource(context, R.mipmap.ic_launcher_round)
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            Icon.createWithBitmap(bitmap)
        }.getOrElse {
            Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        }
    }

    /**
     * Dựng Icon hình tròn màu đỏ đặc #FF5247 với dấu ✕ màu trắng tinh khiết căn giữa.
     */
    fun createRedCrossCircleIcon(context: Context, size: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        // Vòng tròn màu đỏ đặc #FF5247
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF5247")
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Dấu ✕ màu trắng tinh khiết #FFFFFF bo tròn, độ dày size * 0.12f
        val strokeWidth = size * 0.12f
        val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }

        val offset = size * 0.18f
        canvas.drawLine(cx - offset, cy - offset, cx + offset, cy + offset, crossPaint)
        canvas.drawLine(cx - offset, cy + offset, cx + offset, cy - offset, crossPaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon hình tròn màu xanh lá đặc #00D084 với dấu tích ✓ màu trắng tinh khiết căn giữa.
     */
    fun createGreenCheckCircleIcon(context: Context, size: Int = 96): Icon {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        // Vòng tròn màu xanh lá đặc #00D084
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#00D084")
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Dấu tích ✓ màu trắng tinh khiết #FFFFFF bo tròn
        val strokeWidth = size * 0.12f
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }

        val checkPath = Path().apply {
            moveTo(cx - size * 0.20f, cy + size * 0.01f)
            lineTo(cx - size * 0.05f, cy + size * 0.16f)
            lineTo(cx + size * 0.22f, cy - size * 0.15f)
        }
        canvas.drawPath(checkPath, checkPaint)

        return Icon.createWithBitmap(bitmap)
    }

    /**
     * Dựng Icon tròn góc trên bên trái Header (oi_base_icon) cho thẻ mở rộng tai nghe TWS chuẩn OriginOS 6:
     * - Hình tròn nền xám than đậm (#35353C -> #26262B) kèm viền mảnh #1FFFFFFF.
     * - Bên trong là cặp tai nghe in-ear không dây màu trắng tinh (#FFFFFF) đối xứng trái - phải.
     */
    fun createTwsHeaderBadgeIcon(context: Context): Icon {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val cx = 64f
        val cy = 64f
        val radius = 58f

        // 1. Hình tròn nền xám than đậm chuẩn OriginOS 6
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                cx,
                cy - radius,
                cx,
                cy + radius,
                Color.parseColor("#35353C"),
                Color.parseColor("#26262B"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#1FFFFFFF")
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // 2. Cặp tai nghe in-ear không dây màu trắng tinh (#FFFFFF) đối xứng trái - phải
        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2C2C32")
        }

        // Tai trái (bên trái tâm)
        canvas.drawLine(40f, 52f, 45f, 84f, stemPaint)
        canvas.save()
        canvas.rotate(-14f, 44f, 48f)
        canvas.drawOval(RectF(32f, 38f, 56f, 58f), whiteFillPaint)
        canvas.drawOval(RectF(45.5f, 45.5f, 51.5f, 50.5f), meshPaint)
        canvas.restore()

        // Tai phải (bên phải tâm)
        canvas.drawLine(88f, 52f, 83f, 84f, stemPaint)
        canvas.save()
        canvas.rotate(14f, 84f, 48f)
        canvas.drawOval(RectF(72f, 38f, 96f, 58f), whiteFillPaint)
        canvas.drawOval(RectF(76.5f, 45.5f, 82.5f, 50.5f), meshPaint)
        canvas.restore()

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon đơn chiếc tai nghe (Trái hoặc Phải) kèm thanh báo pin đứng chuẩn viên nang OriginOS 6.
     */
    fun createSingleEarbudWithBatteryIcon(
        context: Context,
        isLeft: Boolean,
        level: Int = -1,
        isCharging: Boolean = false,
    ): Icon {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2C2C32")
        }

        if (isLeft) {
            canvas.drawLine(32f, 40f, 36f, 76f, stemPaint)
            canvas.save()
            canvas.rotate(-12f, 38f, 34f)
            canvas.drawOval(RectF(23f, 22f, 53f, 46f), whiteFillPaint)
            canvas.drawOval(RectF(40f, 31f, 47f, 37f), meshPaint)
            canvas.restore()
        } else {
            canvas.drawLine(46f, 40f, 42f, 76f, stemPaint)
            canvas.save()
            canvas.rotate(12f, 40f, 34f)
            canvas.drawOval(RectF(25f, 22f, 55f, 46f), whiteFillPaint)
            canvas.drawOval(RectF(31f, 31f, 38f, 37f), meshPaint)
            canvas.restore()
        }

        // Thanh pin đứng nhỏ gọn bên cạnh tai nghe
        val trackRect = RectF(64f, 24f, 74f, 74f)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#40FFFFFF")
        }
        canvas.drawRoundRect(trackRect, 5f, 5f, trackPaint)

        val clamped = if (level in 0..100) level else 85
        val fillHeight = (trackRect.height() * (clamped / 100f)).coerceAtLeast(6f)
        val fillRect = RectF(trackRect.left, trackRect.bottom - fillHeight, trackRect.right, trackRect.bottom)
        val barColor = when {
            level in 0..15 && !isCharging -> Color.parseColor("#FF3B30")
            isCharging -> Color.parseColor("#00E676")
            else -> Color.parseColor("#2BD968")
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = barColor
        }
        canvas.drawRoundRect(fillRect, 5f, 5f, fillPaint)

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon Hộp sạc 3D màu trắng ngọc trai đang mở nắp (open-lid 3D charging case) chuẩn Poster OriginOS 6:
     * - Lớp 1: Nắp hộp sạc mở phía trên (Open Top Lid) kèm lòng nắp lõm tạo chiều sâu.
     * - Lớp 2: Khay đựng & 2 đầu tai nghe bóng 3D nhô lên từ trong hốc sạc.
     * - Lớp 3: Thân chính hộp sạc phía trước bo khối cầu 3D kèm khe bản lề mở nắp.
     * - Lớp 4: Đèn LED báo pin phát sáng kèm quầng sáng (glow halo) ở chính giữa thân hộp.
     */
    fun createChargingCaseIcon(
        context: Context,
        level: Int = -1,
        isCharging: Boolean = false,
        size: Int = 160,
    ): Icon {
        val bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Lớp 1 — Nắp hộp sạc mở phía trên (Open Top Lid)
        val lidRect = RectF(26f, 18f, 134f, 58f)
        val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                80f,
                18f,
                80f,
                58f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D8DCE3"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(lidRect, 22f, 22f, lidPaint)

        val innerLidRect = RectF(32f, 24f, 128f, 54f)
        val innerLidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                80f,
                24f,
                80f,
                54f,
                Color.parseColor("#CFD4DC"),
                Color.parseColor("#E8ECF2"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(innerLidRect, 16f, 16f, innerLidPaint)

        // Lớp 2 — Khay đựng & 2 đầu tai nghe nhô lên bên trong (Inner Cavity & Left/Right Earbud Heads)
        val trayRect = RectF(30f, 54f, 130f, 76f)
        val trayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#9AA1AE")
        }
        canvas.drawRoundRect(trayRect, 10f, 10f, trayPaint)

        val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#5B6270")
        }
        canvas.drawOval(RectF(36f, 56f, 74f, 74f), socketPaint)
        canvas.drawOval(RectF(86f, 56f, 124f, 74f), socketPaint)

        // Đầu tai nghe trái (Left Earbud Head)
        val leftBudRect = RectF(38f, 44f, 72f, 72f)
        val leftBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                52f,
                52f,
                20f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#DDE2EA"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(leftBudRect, leftBudPaint)

        // Đầu tai nghe phải (Right Earbud Head)
        val rightBudRect = RectF(88f, 44f, 122f, 72f)
        val rightBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                108f,
                52f,
                20f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#DDE2EA"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(rightBudRect, rightBudPaint)

        val meshHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#3A3F47")
        }
        canvas.drawCircle(62f, 58f, 2.8f, meshHolePaint)
        canvas.drawCircle(98f, 58f, 2.8f, meshHolePaint)

        // Lớp 3 — Thân chính hộp sạc phía trước (Front 3D Case Body)
        val bodyRect = RectF(22f, 64f, 138f, 138f)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                80f,
                64f,
                80f,
                138f,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#F2F4F8"),
                    Color.parseColor("#C9CFD9"),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bodyRect, 28f, 28f, bodyPaint)

        // Rãnh bản lề / khe mở nắp mảnh ở mặt trước
        val notchRect = RectF(62f, 64f, 98f, 70f)
        val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#D0D5DF")
        }
        canvas.drawRoundRect(notchRect, 3f, 3f, notchPaint)

        // Đường viền highlight 3D mảnh 1.5f màu #FFFFFF dọc theo mép trên thân hộp
        val topHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.WHITE
        }
        canvas.drawLine(36f, 64.8f, 124f, 64.8f, topHighlightPaint)

        // Lớp 4 — Đèn LED báo pin phát sáng ở chính giữa thân hộp
        val ledColor = when {
            level in 0..15 && !isCharging -> Color.parseColor("#FF3B30")
            isCharging -> Color.parseColor("#00E676")
            else -> Color.parseColor("#2BD968")
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(
                70,
                Color.red(ledColor),
                Color.green(ledColor),
                Color.blue(ledColor),
            )
        }
        canvas.drawCircle(80f, 96f, 9f, haloPaint)

        val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ledColor
        }
        canvas.drawCircle(80f, 96f, 4.2f, ledPaint)

        return Icon.createWithBitmap(bmp)
    }
}
