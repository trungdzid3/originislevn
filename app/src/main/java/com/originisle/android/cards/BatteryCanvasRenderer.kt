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
import android.graphics.Typeface
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
     * Vẽ chi tiết tai nghe in-ear Vivo TWS 3D trắng ngọc trai:
     * - Hình gốc có cuống tai bên trái, củ tai hướng sang phải (dùng trực tiếp cho tai PHẢI).
     * - isLeft = true (tai trái, nằm bên trái đảo): Lật trục X để củ tai hướng sang trái, cuống tai ở bên phải gần tâm đảo.
     * - isLeft = false (tai phải, nằm bên phải đảo): Giữ nguyên để củ tai hướng sang phải, cuống tai ở bên trái gần tâm đảo.
     */
    fun drawInEarBud(
        canvas: Canvas,
        bounds: RectF,
        isLeft: Boolean,
        isCharging: Boolean = false,
    ) {
        val count = canvas.save()
        if (isLeft) {
            // Tai TRÁI: Lật trục X quanh tâm bounds để củ tai hướng ra bên trái, cuống tai nằm phía trong gần tâm
            canvas.scale(-1f, 1f, bounds.centerX(), bounds.centerY())
        }

        val w = bounds.width()
        val h = bounds.height()
        val l = bounds.left
        val t = bounds.top

        // 1. Thân cuống tai (Stem) - Hình trụ bo tròn thanh mảnh
        val stemLeft = l + w * 0.22f
        val stemRight = l + w * 0.38f
        val stemTop = t + h * 0.34f
        val stemBottom = t + h * 0.90f
        val stemRect = RectF(stemLeft, stemTop, stemRight, stemBottom)
        val stemRadius = (stemRight - stemLeft) / 2f

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                stemLeft, stemTop, stemRight, stemTop,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#F4F6F9"),
                    Color.parseColor("#C8CFDC")
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(stemRect, stemRadius, stemRadius, stemPaint)

        // Tiếp điểm sạc kim loại ở đáy cuống tai
        val contactRect = RectF(
            stemLeft + w * 0.02f,
            stemBottom - h * 0.07f,
            stemRight - w * 0.02f,
            stemBottom - h * 0.01f
        )
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#9EA7B5")
        }
        canvas.drawRoundRect(contactRect, stemRadius * 0.7f, stemRadius * 0.7f, contactPaint)

        // Micro ngoài trên thân cuống tai
        val micPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#454A56")
        }
        canvas.drawOval(
            RectF(
                stemLeft + w * 0.05f,
                t + h * 0.62f,
                stemLeft + w * 0.11f,
                t + h * 0.68f
            ),
            micPaint
        )

        // 2. Củ tai (Housing/Earbud Bulb) vươn sang phải
        val headRect = RectF(l + w * 0.20f, t + h * 0.14f, l + w * 0.76f, t + h * 0.58f)
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                l + w * 0.44f,
                t + h * 0.28f,
                w * 0.38f,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#EFF2F7"),
                    Color.parseColor("#CCD3DF")
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(headRect, headPaint)

        // Đệm nhét tai / ống dẫn âm in-ear vươn sang phải
        val tipRect = RectF(l + w * 0.58f, t + h * 0.22f, l + w * 0.90f, t + h * 0.50f)
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                tipRect.left, tipRect.top, tipRect.right, tipRect.bottom,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D4DAE4"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(tipRect, tipRect.height() / 2f, tipRect.height() / 2f, tipPaint)

        // Lưới thoát âm titan hình oval thon gọn ở đỉnh củ tai
        val meshRect = RectF(l + w * 0.76f, t + h * 0.28f, l + w * 0.88f, t + h * 0.44f)
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2B2F38")
        }
        canvas.drawOval(meshRect, meshPaint)

        // Điểm ánh sáng ngọc trai bóng bẩy (Specular Highlight) ở lưng củ tai
        val glintRect = RectF(l + w * 0.30f, t + h * 0.18f, l + w * 0.48f, t + h * 0.28f)
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#80FFFFFF")
        }
        canvas.drawOval(glintRect, glintPaint)

        // Đèn LED báo sạc mini nếu đang sạc
        if (isCharging) {
            val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#00E676")
            }
            canvas.drawCircle(stemLeft + (stemRight - stemLeft) / 2f, t + h * 0.48f, w * 0.035f, ledPaint)
        }

        canvas.restoreToCount(count)
    }

    /**
     * Dựng Icon tai nghe in-ear đơn chiếc chuẩn Đảo viên nang thu gọn Origin Island:
     * - isLeft = true: Cuống tai quay xuống, đầu tai nghe in-ear hướng sang phải.
     * - isLeft = false: Cuống tai quay xuống, đầu tai nghe in-ear hướng sang trái (đối xứng hoàn hảo).
     * - Vẽ màu trắng bóng ngọc trai tinh tế, nét chuẩn xác, chống răng cưa tuyệt đối.
     * - Tùy chọn hiển thị mini level hoặc chấm sạc nếu có.
     */
    fun createBudCapsuleIcon(
        context: Context,
        isLeft: Boolean,
        level: Int = -1,
        isCharging: Boolean = false,
        size: Int = 96
    ): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val padding = size * 0.08f
        val budBounds = RectF(padding, padding, size - padding, size - padding)
        drawInEarBud(canvas, budBounds, isLeft, isCharging)

        // Nếu có level hợp lệ hoặc đang sạc, vẽ chấm pin mini ở góc ngoài
        if (level in 0..100 || isCharging) {
            val dotCx = if (isLeft) size * 0.28f else size * 0.72f
            val dotCy = size * 0.88f
            val dotRadius = size * 0.07f

            val dotColor = when {
                isCharging -> Color.parseColor("#00E676")
                level in 0..15 -> Color.parseColor("#FF5247")
                level in 16..30 -> Color.parseColor("#FFB020")
                else -> Color.parseColor("#00D084")
            }

            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(80, Color.red(dotColor), Color.green(dotColor), Color.blue(dotColor))
            }
            canvas.drawCircle(dotCx, dotCy, dotRadius * 1.5f, haloPaint)

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = dotColor
            }
            canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)
        }

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon tai nghe in-ear đơn chiếc thuần túy (không kèm pin) sắc nét.
     */
    fun createBudOnlyIcon(
        context: Context,
        isLeft: Boolean,
        size: Int = 96
    ): Icon {
        return createPureEarbudIcon(context, isLeft = isLeft, size = size, isCharging = false)
    }

    /**
     * Dựng Icon tai nghe 3D trắng ngọc trai thuần khiết cho viên nang thu gọn hoặc thẻ đơn chiếc:
     * - Xóa hoàn toàn thanh pin xanh lá đứng bên cạnh.
     * - Phóng to tai nghe tràn khung sạch đẹp, giữ nguyên hướng quay chuẩn Trái/Phải.
     */
    fun createPureEarbudIcon(
        context: Context,
        isLeft: Boolean,
        size: Int = 96,
        isCharging: Boolean = false,
    ): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bleed = size * 0.04f
        val bounds = RectF(-bleed, -bleed, size + bleed, size + bleed)
        drawInEarBud(canvas, bounds, isLeft = isLeft, isCharging = isCharging)
        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon tròn góc trên bên trái Header (oi_base_icon) cho thẻ mở rộng tai nghe TWS chuẩn OriginOS 6:
     * - Hình tròn nền xám than đậm (#35353C -> #26262B) kèm viền mảnh #1FFFFFFF.
     * - Bên trong là cặp tai nghe in-ear không dây màu trắng bóng ngọc trai (#FFFFFF) đối xứng trái - phải.
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

        // 2. Cặp tai nghe in-ear không dây bóng 3D đối xứng trái - phải
        val leftBudBounds = RectF(20f, 32f, 62f, 96f)
        val rightBudBounds = RectF(66f, 32f, 108f, 96f)
        drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = false)
        drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = false)

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon ngang kết hợp BIỂU TƯỢNG TAI NGHE IN-EAR 3D + VIÊN PIN NẰM NGANG ORIGIN OS:
     * - Phục vụ cột trái / phải trong thẻ mở rộng đối xứng 3 cột chuẩn Poster Vivo TWS.
     * - isLeft = true: Tai nghe bên trái (quay sang phải) + Viên pin nằm ngang bên phải.
     * - isLeft = false: Viên pin nằm ngang bên trái + Tai nghe bên phải (quay sang trái).
     */
    fun createBudWithBatteryHorizontalIcon(
        context: Context,
        isLeft: Boolean,
        level: Int,
        isCharging: Boolean,
        width: Int = 180,
        height: Int = 90
    ): Icon {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Phân chia khu vực vẽ Tai nghe và Viên pin
        val budBounds: RectF
        val batteryOuterRect: RectF

        if (isLeft) {
            // Tai nghe ở bên trái, Viên pin ở bên phải
            budBounds = RectF(6f, 6f, h * 0.95f, h - 6f)
            val batLeft = budBounds.right + 12f
            val batRight = w - 16f
            val batTop = (h - h * 0.44f) / 2f
            val batBottom = batTop + h * 0.44f
            batteryOuterRect = RectF(batLeft, batTop, batRight, batBottom)
        } else {
            // Viên pin ở bên trái, Tai nghe ở bên phải
            val batLeft = 16f
            val batRight = w - (h * 0.95f) - 12f
            val batTop = (h - h * 0.44f) / 2f
            val batBottom = batTop + h * 0.44f
            batteryOuterRect = RectF(batLeft, batTop, batRight, batBottom)
            budBounds = RectF(batRight + 12f, 6f, w - 6f, h - 6f)
        }

        // Vẽ chiếc tai nghe in-ear 3D
        drawInEarBud(canvas, budBounds, isLeft, isCharging)

        // 2. Vẽ viên pin OriginOS nằm ngang
        val batW = batteryOuterRect.width()
        val batH = batteryOuterRect.height()
        val strokeW = maxOf(2f, batH * 0.08f)
        val cornerRadius = batH * 0.32f

        // Cực dương viên pin
        val capW = maxOf(4f, batW * 0.06f)
        val capH = batH * 0.48f

        val bodyRect = RectF(
            batteryOuterRect.left,
            batteryOuterRect.top,
            batteryOuterRect.right - capW - 2f,
            batteryOuterRect.bottom
        )
        val capRect = RectF(
            bodyRect.right + 2f,
            bodyRect.centerY() - capH / 2f,
            bodyRect.right + 2f + capW,
            bodyRect.centerY() + capH / 2f
        )
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#90FFFFFF")
        }
        canvas.drawRoundRect(capRect, 2f, 2f, capPaint)

        // Khung viền thân pin
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeW
            color = Color.parseColor("#A0FFFFFF")
        }
        val adjustedBodyRect = RectF(
            bodyRect.left + strokeW / 2f,
            bodyRect.top + strokeW / 2f,
            bodyRect.right - strokeW / 2f,
            bodyRect.bottom - strokeW / 2f
        )
        canvas.drawRoundRect(adjustedBodyRect, cornerRadius, cornerRadius, borderPaint)

        // Nền mờ bên trong viên pin
        val bgInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#26FFFFFF")
        }
        canvas.drawRoundRect(adjustedBodyRect, cornerRadius, cornerRadius, bgInnerPaint)

        // Vạch pin lấp đầy (Fill bar)
        val innerPadding = strokeW + 2f
        val fillMaxW = maxOf(0f, bodyRect.width() - innerPadding * 2f)
        val clampedLevel = if (level in 0..100) level else 85
        val fillW = fillMaxW * (clampedLevel / 100f)

        if (fillW > 0f) {
            val fillRect = RectF(
                bodyRect.left + innerPadding,
                bodyRect.top + innerPadding,
                bodyRect.left + innerPadding + fillW,
                bodyRect.bottom - innerPadding
            )
            val fillColor = when {
                isCharging -> Color.parseColor("#00E676")
                clampedLevel in 0..15 -> Color.parseColor("#FF5247")
                clampedLevel in 16..30 -> Color.parseColor("#FFB020")
                else -> Color.parseColor("#00D084")
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            val fillRadius = maxOf(2f, cornerRadius - innerPadding * 0.5f)
            canvas.drawRoundRect(fillRect, fillRadius, fillRadius, fillPaint)
        }

        // Tia sét khi đang sạc
        if (isCharging) {
            val cx = bodyRect.centerX()
            val cy = bodyRect.centerY()
            val boltW = bodyRect.width() * 0.22f
            val boltH = bodyRect.height() * 0.70f

            val boltPath = Path().apply {
                moveTo(cx + boltW * 0.12f, cy - boltH * 0.5f)
                lineTo(cx - boltW * 0.45f, cy + boltH * 0.05f)
                lineTo(cx - boltW * 0.05f, cy + boltH * 0.05f)
                lineTo(cx - boltW * 0.18f, cy + boltH * 0.5f)
                lineTo(cx + boltW * 0.45f, cy - boltH * 0.05f)
                lineTo(cx + boltW * 0.05f, cy - boltH * 0.05f)
                close()
            }
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#50000000")
            }
            canvas.drawPath(boltPath, shadowPaint)

            val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
            canvas.drawPath(boltPath, boltPaint)
        }

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
        size: Int = 96,
    ): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // isLeft = true (nằm ở bên trái đảo): tai nghe ở bên trái (6..62), thanh pin đứng ở bên phải (68..80) hướng về phía tâm đảo
        // isLeft = false (nằm ở bên phải đảo): thanh pin đứng ở bên trái (16..28) hướng về phía tâm đảo, tai nghe ở bên phải (34..90)
        val budBounds = if (isLeft) {
            RectF(6f, 8f, 62f, 88f)
        } else {
            RectF(34f, 8f, 90f, 88f)
        }
        drawInEarBud(canvas, budBounds, isLeft, isCharging)

        // Thanh pin đứng nhỏ gọn bên cạnh tai nghe
        val trackRect = if (isLeft) {
            RectF(68f, 24f, 80f, 76f)
        } else {
            RectF(16f, 24f, 28f, 76f)
        }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#40FFFFFF")
        }
        canvas.drawRoundRect(trackRect, 6f, 6f, trackPaint)

        val clamped = if (level in 0..100) level else 85
        val fillHeight = (trackRect.height() * (clamped / 100f)).coerceAtLeast(6f)
        val fillRect = RectF(trackRect.left, trackRect.bottom - fillHeight, trackRect.right, trackRect.bottom)
        val barColor = when {
            level in 0..15 && !isCharging -> Color.parseColor("#FF5247")
            isCharging -> Color.parseColor("#00E676")
            else -> Color.parseColor("#00D084")
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = barColor
        }
        canvas.drawRoundRect(fillRect, 6f, 6f, fillPaint)

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon Hộp sạc 3D màu trắng ngọc trai đang mở nắp (Open-lid 3D Charging Case) chuẩn Poster OriginOS 6:
     * - Tăng kích thước canvas to hoành tráng (size = 240px mặc định).
     * - Lớp 0: Đổ bóng nền 3D Ambient Drop Shadow tạo chiều sâu lơ lửng.
     * - Lớp 1: Nắp hộp sạc mở ngửa góc rộng phía sau, chiều sâu hốc nắp rõ rệt.
     * - Lớp 2: Khay sạc & 2 đầu tai nghe nhô lên to, rõ bóng bẩy 3D, có lỗ micro/loa kim loại.
     * - Lớp 3: Thân chính hộp sạc phía trước bo tròn mềm mại chuẩn Vivo TWS, rãnh mở nắp mảnh, viền highlight ánh sáng bóng bẩy.
     * - Lớp 4: Đèn LED trung tâm phát sáng rực rỡ với quầng sáng halo glow lớn, màu xanh lá #00D084 (hoặc đỏ cam khi pin yếu / sạc).
     */
    /**
     * Dựng Hộp sạc tai nghe 3D mở nắp trên Canvas với bounds tùy biến:
     * - Lớp 0: Đổ bóng nền 3D Ambient Drop Shadow.
     * - Lớp 1: Nắp hộp sạc mở ngửa góc rộng phía sau, chiều sâu hốc nắp rõ rệt.
     * - Lớp 2: Khay sạc & 2 đầu tai nghe nhô lên to, rõ bóng bẩy 3D, có lỗ micro/loa kim loại.
     * - Lớp 3: Thân chính hộp sạc phía trước bo tròn mềm mại chuẩn Vivo TWS, rãnh mở nắp mảnh, viền highlight ánh sáng bóng bẩy.
     * - Lớp 4: Đèn LED trung tâm phát sáng rực rỡ với quầng sáng halo glow lớn.
     */
    fun drawChargingCaseOnCanvas(
        canvas: Canvas,
        bounds: RectF,
        level: Int = -1,
        isCharging: Boolean = false,
    ) {
        val count = canvas.save()
        val scale = minOf(bounds.width() / 240f, bounds.height() / 240f)
        val offsetX = bounds.left + (bounds.width() - 240f * scale) / 2f
        val offsetY = bounds.top + (bounds.height() - 240f * scale) / 2f
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // Lớp 0 — Đổ bóng nền 3D Ambient Drop Shadow bên dưới đáy hộp sạc
        val shadowRect = RectF(34f, 206f, 206f, 228f)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                120f,
                217f,
                90f,
                Color.parseColor("#38000000"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(shadowRect, shadowPaint)

        // Lớp 1 — Nắp hộp sạc mở ngửa góc rộng phía sau (Open Top Lid)
        val lidRect = RectF(30f, 16f, 210f, 82f)
        val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                120f,
                16f,
                120f,
                82f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#C4CAD6"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(lidRect, 36f, 36f, lidPaint)

        // Hốc lõm nắp bên trong (Inner Lid Cavity) tạo chiều sâu 3D mở ngửa ra sau
        val innerLidRect = RectF(40f, 24f, 200f, 76f)
        val innerLidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                120f,
                24f,
                120f,
                76f,
                Color.parseColor("#ABB2C0"),
                Color.parseColor("#E0E5EE"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(innerLidRect, 28f, 28f, innerLidPaint)

        // Bản lề nắp sau
        val hingeRect = RectF(90f, 74f, 150f, 84f)
        val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#7B8394")
        }
        canvas.drawRoundRect(hingeRect, 4f, 4f, hingePaint)

        // Lớp 2 — Khay sạc & 2 đầu tai nghe nhô lên to, rõ bóng bẩy 3D
        val trayRect = RectF(36f, 72f, 204f, 110f)
        val trayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#828A9B")
        }
        canvas.drawRoundRect(trayRect, 18f, 18f, trayPaint)

        // 2 Hốc cắm tai nghe sâu
        val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#3B3F4B")
        }
        canvas.drawOval(RectF(46f, 74f, 110f, 106f), socketPaint)
        canvas.drawOval(RectF(130f, 74f, 194f, 106f), socketPaint)

        // Đầu tai nghe trái (Left Earbud Head) nhô lên 3D
        val leftBudRect = RectF(50f, 50f, 106f, 102f)
        val leftBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                70f,
                64f,
                38f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#CCD2DE"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(leftBudRect, leftBudPaint)

        // Đầu tai nghe phải (Right Earbud Head) nhô lên 3D
        val rightBudRect = RectF(134f, 50f, 190f, 102f)
        val rightBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                170f,
                64f,
                38f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#CCD2DE"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(rightBudRect, rightBudPaint)

        // Lưới loa / micro kim loại titan trên 2 đầu tai nghe
        val meshHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#262A33")
        }
        canvas.drawOval(RectF(82f, 72f, 96f, 84f), meshHolePaint)
        canvas.drawOval(RectF(144f, 72f, 158f, 84f), meshHolePaint)

        // Điểm phản chiếu ánh ngọc trai trên 2 đầu tai nghe
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#90FFFFFF")
        }
        canvas.drawCircle(68f, 62f, 4.5f, glintPaint)
        canvas.drawCircle(172f, 62f, 4.5f, glintPaint)

        // Lớp 3 — Thân chính hộp sạc phía trước (Front 3D Case Body)
        // Bo tròn mềm mại chuẩn Vivo TWS
        val bodyRect = RectF(22f, 88f, 218f, 210f)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                120f,
                88f,
                120f,
                210f,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#F4F6F9"),
                    Color.parseColor("#E0E5EE"),
                    Color.parseColor("#BCC3D1"),
                ),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bodyRect, 46f, 46f, bodyPaint)

        // Rãnh mở nắp mảnh ở mép trên thân hộp
        val notchRect = RectF(94f, 88f, 146f, 96f)
        val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#CBD1DC")
        }
        canvas.drawRoundRect(notchRect, 4f, 4f, notchPaint)

        // Đường viền highlight ánh sáng bóng bẩy sắc nét
        val topHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.WHITE
        }
        canvas.drawLine(46f, 89.2f, 194f, 89.2f, topHighlightPaint)

        // Đèn LED trung tâm phát sáng rực rỡ với quầng sáng (halo glow) lớn
        val ledColor = when {
            isCharging -> Color.parseColor("#00E676")
            level in 0..15 -> Color.parseColor("#FF5247")
            level in 16..30 -> Color.parseColor("#FFB020")
            else -> Color.parseColor("#00D084")
        }

        val cx = 120f
        val cy = 132f

        // Vòng quầng sáng lớn ngoài cùng (Outer Halo)
        val outerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(
                45,
                Color.red(ledColor),
                Color.green(ledColor),
                Color.blue(ledColor),
            )
        }
        canvas.drawCircle(cx, cy, 22f, outerHaloPaint)

        // Vòng quầng sáng trung gian (Mid Halo)
        val midHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(
                95,
                Color.red(ledColor),
                Color.green(ledColor),
                Color.blue(ledColor),
            )
        }
        canvas.drawCircle(cx, cy, 13f, midHaloPaint)

        // Lõi đèn LED sắc nét
        val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ledColor
        }
        canvas.drawCircle(cx, cy, 5.8f, ledPaint)

        // Đốm phát quang trắng ở tâm đèn LED
        val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, 2.2f, centerGlowPaint)

        // Số % pin case to rõ nét ở thân hộp sạc (dưới đèn LED)
        if (level in 0..100) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2C3038")
                textSize = 28f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val text = "$level%"
            canvas.drawText(text, cx, 172f, textPaint)
        }

        canvas.restoreToCount(count)
    }

    /**
     * Dựng Icon Cặp tai nghe 3D phóng to khổng lồ chiếm trọn khung hình (không có hộp sạc)
     * hiển thị khi đã lấy cả 2 tai nghe ra khỏi hộp sạc.
     */
    fun createDualBudsLargeIcon(
        context: Context,
        leftLevel: Int = -1,
        rightLevel: Int = -1,
        isLeftCharging: Boolean = false,
        isRightCharging: Boolean = false,
        size: Int = 240,
    ): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val s = size.toFloat()
        // Tai trái: chiếm nửa trái, củ tai vươn vào tâm
        val leftBudBounds = RectF(s * 0.04f, s * 0.08f, s * 0.48f, s * 0.92f)
        drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)

        // Tai phải: chiếm nửa phải, củ tai vươn vào tâm
        val rightBudBounds = RectF(s * 0.52f, s * 0.08f, s * 0.96f, s * 0.92f)
        drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon Hộp sạc 3D đơn chiếc:
     */
    fun createChargingCaseIcon(
        context: Context,
        level: Int = -1,
        isCharging: Boolean = false,
        size: Int = 240,
    ): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawChargingCaseOnCanvas(canvas, RectF(0f, 0f, size.toFloat(), size.toFloat()), level, isCharging)
        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Banner 3D Toàn cảnh gồm cả 3 thiết bị chuẩn 100% Poster OriginOS 6:
     * - Bên trái: Chiếc tai nghe TRÁI 3D (củ tai hướng sang trái, cuống tai bên phải)
     * - Ở giữa: Chiếc Hộp sạc 3D mở nắp rộng rãi, bóng bẩy ngọc trai, đèn LED phát sáng
     * - Bên phải: Chiếc tai nghe PHẢI 3D (củ tai hướng sang phải, cuống tai bên trái)
     * Kích thước 600x200 px, hiển thị ở tâm Thẻ mở rộng để mỗi hình nằm chuẩn xác phía trên từng cột tương ứng!
     */
    fun createTwsTrioBannerIcon(
        context: Context,
        leftLevel: Int = -1,
        rightLevel: Int = -1,
        caseLevel: Int = -1,
        isLeftCharging: Boolean = false,
        isRightCharging: Boolean = false,
        isCaseCharging: Boolean = false,
        width: Int = 600,
        height: Int = 200,
    ): Icon {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val colW = width / 3f

        // Cột 1 (Bên trái): Chiếc tai nghe TRÁI 3D
        // Củ tai hướng sang TRÁI, cuống tai ở bên PHẢI
        val leftBudBounds = RectF(colW * 0.12f, height * 0.08f, colW * 0.88f, height * 0.92f)
        drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)

        // Cột 2 (Ở giữa): Chiếc Hộp sạc 3D mở nắp
        val caseBounds = RectF(colW + colW * 0.08f, height * 0.04f, colW * 2f - colW * 0.08f, height * 0.96f)
        drawChargingCaseOnCanvas(canvas, caseBounds, caseLevel, isCaseCharging)

        // Cột 3 (Bên phải): Chiếc tai nghe PHẢI 3D
        // Củ tai hướng sang PHẢI, cuống tai ở bên TRÁI
        val rightBudBounds = RectF(colW * 2f + colW * 0.12f, height * 0.08f, width - colW * 0.12f, height * 0.92f)
        drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Vẽ hộp sạc 3D trắng ngọc trai mở nắp cao tràn khung cho ảnh Composite thẻ mở rộng:
     * - Nắp trên và lòng nắp dùng sắc trắng/bạc sáng nổi bật trên nền thẻ tối (tuyệt đối không dùng màu tối).
     * - Có khay sạc ánh bạc, đèn LED phát sáng ở thân case, không vẽ chữ % đè lên thân hộp.
     */
    private fun drawOpenChargingCaseForComposite(
        canvas: Canvas,
        bounds: RectF,
        level: Int,
        isCharging: Boolean,
    ) {
        val l = bounds.left
        val t = bounds.top
        val w = bounds.width()
        val h = bounds.height()

        // 1. Nắp hộp mở ngửa cao tràn sát mép trên
        val lidRect = RectF(l + w * 0.08f, t + h * 0.01f, l + w * 0.92f, t + h * 0.45f)
        val lidRadius = w * 0.24f
        val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                lidRect.centerX(),
                lidRect.top,
                lidRect.centerX(),
                lidRect.bottom,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#EEF2F8"),
                    Color.parseColor("#D5DCE8"),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(lidRect, lidRadius, lidRadius, lidPaint)

        // Viền sáng mảnh ở mép nắp để tách bạch hoàn toàn khỏi nền đen
        val lidRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1.4f, w * 0.012f)
            color = Color.parseColor("#E6FFFFFF")
        }
        canvas.drawRoundRect(lidRect, lidRadius, lidRadius, lidRimPaint)

        // Lòng nắp mở lõm bên trong (tông bạc ngọc trai sáng, không tối)
        val innerLidRect = RectF(l + w * 0.14f, t + h * 0.055f, l + w * 0.86f, t + h * 0.40f)
        val innerLidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                innerLidRect.centerX(),
                innerLidRect.top,
                innerLidRect.centerX(),
                innerLidRect.bottom,
                Color.parseColor("#CBD3E2"),
                Color.parseColor("#F5F8FC"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(innerLidRect, w * 0.19f, w * 0.19f, innerLidPaint)

        // 2. Khay giữa & 2 hốc sạc ánh bạc
        val trayRect = RectF(l + w * 0.06f, t + h * 0.36f, l + w * 0.94f, t + h * 0.50f)
        val trayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                trayRect.centerX(),
                trayRect.top,
                trayRect.centerX(),
                trayRect.bottom,
                Color.parseColor("#B6C0D0"),
                Color.parseColor("#DCE2EC"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(trayRect, w * 0.12f, w * 0.12f, trayPaint)

        val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#929CB0")
        }
        canvas.drawOval(RectF(l + w * 0.17f, t + h * 0.38f, l + w * 0.45f, t + h * 0.46f), wellPaint)
        canvas.drawOval(RectF(l + w * 0.55f, t + h * 0.38f, l + w * 0.83f, t + h * 0.46f), wellPaint)

        val hingeRect = RectF(l + w * 0.36f, t + h * 0.385f, l + w * 0.64f, t + h * 0.435f)
        val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#A4AEBF")
        }
        canvas.drawRoundRect(hingeRect, w * 0.03f, w * 0.03f, hingePaint)

        // 3. Thân chính hộp sạc trắng ngọc trai phía trước (tràn sát mép dưới)
        val bodyRect = RectF(l + w * 0.04f, t + h * 0.43f, l + w * 0.96f, t + h * 0.98f)
        val bodyRadius = w * 0.28f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                bodyRect.centerX(),
                bodyRect.top,
                bodyRect.centerX(),
                bodyRect.bottom,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#F4F7FB"),
                    Color.parseColor("#E1E6F0"),
                    Color.parseColor("#C2CAD8"),
                ),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, bodyPaint)

        // Rãnh mở nắp & đường viền highlight trên mép thân hộp
        val notchRect = RectF(l + w * 0.38f, t + h * 0.43f, l + w * 0.62f, t + h * 0.475f)
        val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#CBD3E0")
        }
        canvas.drawRoundRect(notchRect, w * 0.03f, w * 0.03f, notchPaint)

        val topHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1.5f, w * 0.015f)
            color = Color.WHITE
        }
        canvas.drawLine(
            l + w * 0.20f,
            t + h * 0.436f,
            l + w * 0.80f,
            t + h * 0.436f,
            topHighlightPaint,
        )

        // 4. Đèn LED trạng thái phát sáng rõ ở thân case (không vẽ text % đè lên hộp)
        val ledColor = when {
            isCharging -> Color.parseColor("#00E676")
            level in 0..15 -> Color.parseColor("#FF5247")
            level in 16..30 -> Color.parseColor("#FFB020")
            else -> Color.parseColor("#00D084")
        }
        val ledCx = bodyRect.centerX()
        val ledCy = t + h * 0.695f

        val outerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(58, Color.red(ledColor), Color.green(ledColor), Color.blue(ledColor))
        }
        canvas.drawCircle(ledCx, ledCy, w * 0.14f, outerHaloPaint)

        val midHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(120, Color.red(ledColor), Color.green(ledColor), Color.blue(ledColor))
        }
        canvas.drawCircle(ledCx, ledCy, w * 0.085f, midHaloPaint)

        val ledCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ledColor
        }
        canvas.drawCircle(ledCx, ledCy, w * 0.042f, ledCorePaint)

        val ledGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(ledCx, ledCy, w * 0.016f, ledGlintPaint)
    }

    /**
     * Dựng ảnh Hộp sạc 3D mở nắp vuông 1:1 (192x192 px) chuẩn OriginOS 6 / Apple AirPods Pro:
     * - Thân hộp sạc trắng ngọc trai bo tròn squircle nổi bật trên nền tối #141519.
     * - Nắp hộp mở ngửa, khoang lõm bạc với 2 hốc giữ tai nghe đối xứng.
     * - 2 đầu tai nghe trắng bóng nhô nhẹ trong lòng hộp sạc.
     * - Rãnh bản lề kim loại và đèn LED phát sáng ở chính giữa thân hộp.
     */
    /**
     * Vẽ Hộp sạc 3D mở nắp vuông (tỷ lệ chuẩn 192x192 px) lên canvas với bounds tùy biến.
     */
    fun drawChargingCase3DOnCanvas(
        canvas: Canvas,
        bounds: RectF,
        casePct: Int = -1,
        isCharging: Boolean = false,
    ) {
        val count = canvas.save()
        val size = 192f
        val scale = minOf(bounds.width() / size, bounds.height() / size)
        val offsetX = bounds.left + (bounds.width() - size * scale) / 2f
        val offsetY = bounds.top + (bounds.height() - size * scale) / 2f
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // 1. Nắp hộp sạc mở phía trên (Open Top Lid)
        val lidRect = RectF(24f, 18f, 168f, 82f)
        val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                96f,
                18f,
                96f,
                82f,
                Color.parseColor("#F8FAFC"),
                Color.parseColor("#DCE2EC"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(lidRect, 34f, 34f, lidPaint)

        // 2. Khoang lõm bên trong nắp (Inner Lid Cavity)
        val innerLidRect = RectF(34f, 26f, 158f, 78f)
        val innerLidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#C5CEDB")
        }
        canvas.drawRoundRect(innerLidRect, 26f, 26f, innerLidPaint)

        // 2 hốc lõm tối đối xứng trái/phải mô phỏng khuôn giữ tai nghe trong nắp hộp
        val cavityRecessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#8E98A8")
        }
        canvas.drawOval(RectF(48f, 34f, 88f, 64f), cavityRecessPaint)
        canvas.drawOval(RectF(104f, 34f, 144f, 64f), cavityRecessPaint)

        // 3. 2 đầu tai nghe trắng bóng nhô nhẹ trong hốc sạc (Seated Buds inside Case)
        val cxLeft = 68f
        val cxRight = 124f
        val budCy = 70f
        val budRadius = 20f

        val leftBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                cxLeft - 4f,
                budCy - 5f,
                budRadius * 1.2f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D8E0EC"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cxLeft, budCy, budRadius, leftBudPaint)

        val rightBudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                cxRight + 4f,
                budCy - 5f,
                budRadius * 1.2f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D8E0EC"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cxRight, budCy, budRadius, rightBudPaint)

        // Khe loa đen mảnh hướng vào trong trên mỗi đầu tai nghe
        val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#1A1D24")
        }
        canvas.drawOval(RectF(cxLeft + 6f, 65f, cxLeft + 13f, 73f), slotPaint)
        canvas.drawOval(RectF(cxRight - 13f, 65f, cxRight - 6f, 73f), slotPaint)

        // 4. Thân hộp sạc dưới (Lower Case Body) vẽ đè lên phần dưới của 2 tai nghe
        val bodyRect = RectF(20f, 78f, 172f, 174f)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                96f,
                78f,
                96f,
                174f,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#EAEEF5"),
                    Color.parseColor("#D0D7E3"),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bodyRect, 36f, 36f, bodyPaint)

        // Viền highlight mảnh 1.5f màu #FFFFFF để nổi bật 100% trên nền thẻ đen #141519
        val bodyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#FFFFFF")
        }
        canvas.drawRoundRect(bodyRect, 36f, 36f, bodyBorderPaint)

        // 5. Rãnh bản lề kim loại (Metallic Hinge Lip) & đường khớp nối mảnh ngang tại y = 78f
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.parseColor("#B8C0CC")
        }
        canvas.drawLine(34f, 78f, 158f, 78f, seamPaint)

        val hingeLipRect = RectF(74f, 78f, 118f, 85f)
        val hingeLipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#B0B8C6")
        }
        canvas.drawRoundRect(hingeLipRect, 3.5f, 3.5f, hingeLipPaint)

        // 6. Đèn báo LED phát sáng (Front LED Indicator) tại chính giữa thân hộp (cx = 96f, cy = 122f)
        val ledCx = 96f
        val ledCy = 122f
        val isLowBattery = casePct in 1..19
        val ledCoreColor = when {
            isCharging -> Color.parseColor("#00E676")
            isLowBattery -> Color.parseColor("#FF5247")
            else -> Color.parseColor("#00D084")
        }
        val ledHaloStart = when {
            isCharging -> Color.parseColor("#6600E676")
            isLowBattery -> Color.parseColor("#66FF5247")
            else -> Color.parseColor("#6600D084")
        }
        val ledHaloEnd = when {
            isCharging -> Color.parseColor("#0000E676")
            isLowBattery -> Color.parseColor("#00FF5247")
            else -> Color.parseColor("#0000D084")
        }

        val ledHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                ledCx,
                ledCy,
                12f,
                ledHaloStart,
                ledHaloEnd,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(ledCx, ledCy, 12f, ledHaloPaint)

        val ledCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ledCoreColor
        }
        canvas.drawCircle(ledCx, ledCy, 4.5f, ledCorePaint)

        val ledWhiteCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#CCFFFFFF")
        }
        canvas.drawCircle(ledCx, ledCy, 1.6f, ledWhiteCenterPaint)

        canvas.restoreToCount(count)
    }

    fun createChargingCase3DIcon(
        context: Context,
        casePct: Int = -1,
    ): Icon {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawChargingCase3DOnCanvas(canvas, RectF(0f, 0f, size.toFloat(), size.toFloat()), casePct)
        return Icon.createWithBitmap(bmp)
    }

    /**
     * Helper nội bộ vẽ nhãn phần trăm pin + icon viên pin nằm ngang chuẩn OriginOS 6:
     * - Căn giữa theo trục cx tại tâm dọc cy.
     * - Dòng chữ trắng sắc nét label ("Trái 85%", "Hộp sạc 50%", "Phải 90%"), textSize = 22f, Typeface.DEFAULT_BOLD, #F4F6FA.
     * - Ngay bên phải chữ cách 8f là icon viên pin nằm ngang (30x16f, bo góc 4f, viền #88FFFFFF, cực dương bên phải).
     * - Tô màu pin theo pct: #00D084 (hoặc #FF5247 nếu pct <= 15).
     * - Có tia sét nhỏ vàng #FFD54F nếu isCharging.
     */
    private fun drawHorizontalBatteryBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        label: String,
        pct: Int,
        isCharging: Boolean,
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F6FA")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }

        val textWidth = textPaint.measureText(label)
        val spacing = 8f
        val batWidth = 30f
        val batHeight = 16f
        val totalWidth = textWidth + spacing + batWidth
        val startX = cx - totalWidth / 2f

        // 1. Dòng chữ label
        val fontMetrics = textPaint.fontMetrics
        val textBaseY = cy - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(label, startX, textBaseY, textPaint)

        // 2. Icon viên pin nằm ngang bên phải chữ
        val batLeft = startX + textWidth + spacing
        val batTop = cy - batHeight / 2f
        val batBottom = cy + batHeight / 2f

        val bodyWidth = 26f
        val capWidth = 3f
        val capGap = 1f

        val bodyRect = RectF(batLeft, batTop, batLeft + bodyWidth, batBottom)
        val bodyRadius = 4f

        // Viền pin mờ #88FFFFFF
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.parseColor("#88FFFFFF")
        }
        val insetBodyRect = RectF(
            bodyRect.left + 0.6f,
            bodyRect.top + 0.6f,
            bodyRect.right - 0.6f,
            bodyRect.bottom - 0.6f
        )
        canvas.drawRoundRect(insetBodyRect, bodyRadius, bodyRadius, borderPaint)

        // Đầu cực dương nhỏ bên phải
        val capLeft = batLeft + bodyWidth + capGap
        val capTop = cy - 4f
        val capRight = capLeft + capWidth
        val capBottom = cy + 4f
        val capRect = RectF(capLeft, capTop, capRight, capBottom)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#88FFFFFF")
        }
        canvas.drawRoundRect(capRect, 1.5f, 1.5f, capPaint)

        // Tô ruột viên pin theo pct
        val innerPadding = 2f
        val fillMaxW = (bodyWidth - innerPadding * 2f).coerceAtLeast(0f)
        val clampedPct = pct.coerceIn(0, 100)
        val fillW = fillMaxW * (clampedPct / 100f)

        if (fillW > 0f) {
            val fillRect = RectF(
                bodyRect.left + innerPadding,
                bodyRect.top + innerPadding,
                bodyRect.left + innerPadding + fillW,
                bodyRect.bottom - innerPadding
            )
            val fillColor = if (clampedPct <= 15) Color.parseColor("#FF5247") else Color.parseColor("#00D084")
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            val innerRadius = (bodyRadius - 1f).coerceAtLeast(1.5f)
            canvas.drawRoundRect(fillRect, innerRadius, innerRadius, fillPaint)
        }

        // Tia sét nhỏ vàng #FFD54F nếu isCharging
        if (isCharging) {
            val boltCx = bodyRect.centerX()
            val boltCy = bodyRect.centerY()
            val boltW = 6f
            val boltH = 10f

            val boltPath = Path().apply {
                moveTo(boltCx + boltW * 0.15f, boltCy - boltH * 0.5f)
                lineTo(boltCx - boltW * 0.5f, boltCy + boltH * 0.05f)
                lineTo(boltCx - boltW * 0.05f, boltCy + boltH * 0.05f)
                lineTo(boltCx - boltW * 0.2f, boltCy + boltH * 0.5f)
                lineTo(boltCx + boltW * 0.5f, boltCy - boltH * 0.05f)
                lineTo(boltCx + boltW * 0.05f, boltCy - boltH * 0.05f)
                close()
            }

            val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#FFD54F")
            }
            canvas.drawPath(boltPath, boltPaint)
        }
    }

    /**
     * Dựng Banner 3D toàn khung rộng 600x210 px cho thẻ mở rộng tai nghe TWS trên OriginOS 6 SuperX (oi_sub_image).
     * - Kích thước Bitmap: 600 x 210 px (ARGB_8888, an toàn Binder IPC dưới 1MB).
     * - Nền banner: Bo góc 28f, màu kính OriginOS (#EB16181E) kèm viền hairline mảnh 1.2f (#1CFFFFFF).
     * - Tự động bố cục theo 3 kịch bản hiển thị:
     *   + Kịch bản A: 3 cột đối xứng (Tai Trái - Hộp sạc - Tai Phải) khi có đầy đủ 3 thiết bị.
     *   + Kịch bản B: 2 cột đối xứng phóng to khi 2 tai nghe đã ra khỏi hộp (ẩn hộp sạc).
     *   + Kịch bản C: Chỉ đeo 1 bên tai nghe (ghép đôi Tai nghe + Hộp sạc hoặc chỉ 1 Tai nghe giữa banner).
     */
    fun createEarbudsExpandedBanner(
        context: Context,
        leftPct: Int,
        rightPct: Int,
        casePct: Int,
        isLeftCharging: Boolean = false,
        isRightCharging: Boolean = false,
        isCaseCharging: Boolean = false,
    ): Icon {
        val width = 600
        val height = 210
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Nền kính bán trong suốt chuẩn OriginOS
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(235, 22, 24, 30) // #EB16181E
        }
        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bgRect, 28f, 28f, bgPaint)

        // Viền hairline mảnh 1.2f
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.argb(28, 255, 255, 255)
        }
        val borderRect = RectF(0.6f, 0.6f, width - 0.6f, height - 0.6f)
        canvas.drawRoundRect(borderRect, 28f, 28f, borderPaint)

        // 2. Xác định trạng thái thiết bị
        val hasLeft = leftPct in 0..100
        val hasRight = rightPct in 0..100
        val hasCase = casePct in 0..100

        when {
            // Kịch bản A (hasLeft && hasRight && hasCase) — 3 cột đối xứng
            hasLeft && hasRight && hasCase -> {
                val budW = 82f
                val budH = 115f

                // Cột Trái (cx = 110f): Tai nghe Trái 3D Pearl White
                val leftBudBounds = RectF(110f - budW / 2f, 78f - budH / 2f, 110f + budW / 2f, 78f + budH / 2f)
                drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)
                drawHorizontalBatteryBadge(canvas, 110f, 172f, "Trái $leftPct%", leftPct, isLeftCharging)

                // Cột Giữa (cx = 300f): Hộp sạc 3D mở nắp LỚN (~124x124f)
                val caseBounds = RectF(300f - 62f, 78f - 62f, 300f + 62f, 78f + 62f)
                drawChargingCase3DOnCanvas(canvas, caseBounds, casePct, isCaseCharging)
                drawHorizontalBatteryBadge(canvas, 300f, 172f, "Hộp sạc $casePct%", casePct, isCaseCharging)

                // Cột Phải (cx = 490f): Tai nghe Phải 3D Pearl White
                val rightBudBounds = RectF(490f - budW / 2f, 78f - budH / 2f, 490f + budW / 2f, 78f + budH / 2f)
                drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)
                drawHorizontalBatteryBadge(canvas, 490f, 172f, "Phải $rightPct%", rightPct, isRightCharging)
            }

            // Kịch bản B (hasLeft && hasRight && !hasCase) — Đã lấy 2 tai nghe khỏi hộp (Ẩn Case)
            hasLeft && hasRight && !hasCase -> {
                val budW = 91f
                val budH = 128f

                // Cột Trái (cx = 175f): Tai nghe Trái 3D phóng to
                val leftBudBounds = RectF(175f - budW / 2f, 80f - budH / 2f, 175f + budW / 2f, 80f + budH / 2f)
                drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)
                drawHorizontalBatteryBadge(canvas, 175f, 174f, "Trái $leftPct%", leftPct, isLeftCharging)

                // Cột Phải (cx = 425f): Tai nghe Phải 3D phóng to
                val rightBudBounds = RectF(425f - budW / 2f, 80f - budH / 2f, 425f + budW / 2f, 80f + budH / 2f)
                drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)
                drawHorizontalBatteryBadge(canvas, 425f, 174f, "Phải $rightPct%", rightPct, isRightCharging)
            }

            // Kịch bản C (hasLeft != hasRight) — Chỉ đeo 1 bên tai nghe
            hasLeft != hasRight -> {
                if (hasCase) {
                    val budW = 91f
                    val budH = 128f

                    // Cột 1 (cx = 175f): Tai nghe đang đeo
                    if (hasLeft) {
                        val leftBudBounds = RectF(175f - budW / 2f, 80f - budH / 2f, 175f + budW / 2f, 80f + budH / 2f)
                        drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)
                        drawHorizontalBatteryBadge(canvas, 175f, 174f, "Trái $leftPct%", leftPct, isLeftCharging)
                    } else {
                        val rightBudBounds = RectF(175f - budW / 2f, 80f - budH / 2f, 175f + budW / 2f, 80f + budH / 2f)
                        drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)
                        drawHorizontalBatteryBadge(canvas, 175f, 174f, "Phải $rightPct%", rightPct, isRightCharging)
                    }

                    // Cột 2 (cx = 425f): Hộp sạc 3D
                    val caseBounds = RectF(425f - 62f, 78f - 62f, 425f + 62f, 78f + 62f)
                    drawChargingCase3DOnCanvas(canvas, caseBounds, casePct, isCaseCharging)
                    drawHorizontalBatteryBadge(canvas, 425f, 174f, "Hộp sạc $casePct%", casePct, isCaseCharging)
                } else {
                    // Không có case: vẽ tai nghe duy nhất ở chính giữa (cx = 300f)
                    val budW = 94f
                    val budH = 132f
                    val centerBudBounds = RectF(300f - budW / 2f, 80f - budH / 2f, 300f + budW / 2f, 80f + budH / 2f)

                    if (hasLeft) {
                        drawInEarBud(canvas, centerBudBounds, isLeft = true, isCharging = isLeftCharging)
                        drawHorizontalBatteryBadge(canvas, 300f, 174f, "Tai trái $leftPct%", leftPct, isLeftCharging)
                    } else {
                        drawInEarBud(canvas, centerBudBounds, isLeft = false, isCharging = isRightCharging)
                        drawHorizontalBatteryBadge(canvas, 300f, 174f, "Tai phải $rightPct%", rightPct, isRightCharging)
                    }
                }
            }

            // Trường hợp chỉ có Hộp sạc
            hasCase -> {
                val caseBounds = RectF(300f - 62f, 78f - 62f, 300f + 62f, 78f + 62f)
                drawChargingCase3DOnCanvas(canvas, caseBounds, casePct, isCaseCharging)
                drawHorizontalBatteryBadge(canvas, 300f, 172f, "Hộp sạc $casePct%", casePct, isCaseCharging)
            }

            // Fallback mặc định
            else -> {
                val budW = 82f
                val budH = 115f
                val leftBudBounds = RectF(110f - budW / 2f, 78f - budH / 2f, 110f + budW / 2f, 78f + budH / 2f)
                drawInEarBud(canvas, leftBudBounds, isLeft = true, isCharging = isLeftCharging)
                drawHorizontalBatteryBadge(canvas, 110f, 172f, "Trái $leftPct%", leftPct, isLeftCharging)

                val caseBounds = RectF(300f - 62f, 78f - 62f, 300f + 62f, 78f + 62f)
                drawChargingCase3DOnCanvas(canvas, caseBounds, casePct, isCaseCharging)
                drawHorizontalBatteryBadge(canvas, 300f, 172f, "Hộp sạc $casePct%", casePct, isCaseCharging)

                val rightBudBounds = RectF(490f - budW / 2f, 78f - budH / 2f, 490f + budW / 2f, 78f + budH / 2f)
                drawInEarBud(canvas, rightBudBounds, isLeft = false, isCharging = isRightCharging)
                drawHorizontalBatteryBadge(canvas, 490f, 172f, "Phải $rightPct%", rightPct, isRightCharging)
            }
        }

        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon tai nghe đơn chiếc 3D Pearl White độ phân giải cao (192x192 px):
     * - Chiếm ~82% chiều cao khung 192x192, không có thanh pin xanh lá dưới chân.
     * - isLeft = true: đầu tai & nút silicone hướng vào trong (sang phải).
     * - isLeft = false: đầu tai & nút silicone hướng vào trong (sang trái).
     */
    fun createSingleEarbudIcon(context: Context, isLeft: Boolean): Icon {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        canvas.save()
        if (!isLeft) {
            // Tai phải: lật trục X quanh tâm để đầu tai hướng sang trái (vào trong)
            canvas.scale(-1f, 1f, size / 2f, size / 2f)
        }

        // Vẽ chuẩn ở hướng tai trái (cuống bên trái, đầu tai & nút silicone hướng sang phải)
        // Chiếm ~82% chiều cao (từ y = 17f đến y = 175f => 158f / 192f = 82.3%)

        // 1. Thân cuống tai (Stem) với bóng khối 3D LinearGradient
        val stemRect = RectF(48f, 58f, 76f, 173f)
        val stemRadius = 14f
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                stemRect.left,
                stemRect.top,
                stemRect.right,
                stemRect.top,
                intArrayOf(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#F1F4F9"),
                    Color.parseColor("#D6DCE8"),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(stemRect, stemRadius, stemRadius, stemPaint)

        // Vạch cảm ứng lực (Force-touch groove) trên thân cuống
        val forceGroovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#C2CAD8")
        }
        canvas.drawRoundRect(RectF(50f, 108f, 55f, 134f), 2.5f, 2.5f, forceGroovePaint)

        // Khuyên sạc bạc ở đuôi cuống
        val ringRect = RectF(50f, 162f, 74f, 174f)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                ringRect.left,
                ringRect.top,
                ringRect.right,
                ringRect.top,
                Color.parseColor("#CBD2DE"),
                Color.parseColor("#8D97A8"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(ringRect, 6f, 6f, ringPaint)

        // 2. Củ tai 3D Pearl White (Earbud Housing)
        val housingRect = RectF(44f, 18f, 128f, 94f)
        val housingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                76f,
                42f,
                54f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D6DCE8"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(housingRect, housingPaint)

        // Điểm bóng phản chiếu (Specular highlight) trên lưng củ tai
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#88FFFFFF")
        }
        canvas.drawOval(RectF(56f, 26f, 88f, 44f), highlightPaint)

        // Chấm cảm biến (Proximity sensor dot) #252830
        val sensorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#252830")
        }
        canvas.drawCircle(92f, 74f, 5f, sensorPaint)

        // 3. Nút tai silicone (Silicone Ear Tip) hướng sang phải (vào trong)
        val tipPath = Path().apply {
            moveTo(110f, 34f)
            cubicTo(132f, 28f, 152f, 38f, 152f, 56f)
            cubicTo(152f, 74f, 132f, 84f, 110f, 78f)
            close()
        }
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                110f,
                30f,
                152f,
                82f,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#DCE2EE"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(tipPath, tipPaint)

        // Lưới âm thanh bầu dục đen #1A1D24 ở đầu nút tai
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#1A1D24")
        }
        canvas.drawOval(RectF(134f, 43f, 148f, 69f), meshPaint)

        canvas.restore()
        return Icon.createWithBitmap(bmp)
    }

    /**
     * Dựng Icon Hộp sạc 3D cho ô vuông chính giữa (oi_large_icon) của Thẻ mở rộng TWS.
     */
    fun createTwsExpandedCompositeIcon(
        context: Context,
        leftPct: Int,
        rightPct: Int,
        casePct: Int,
    ): Icon {
        return createChargingCase3DIcon(context, casePct)
    }

    /**
     * Overload tương thích ngược cho caller cũ: trả về duy nhất Hộp sạc 3D 192x192.
     */
    fun createTwsExpandedCompositeIcon(
        context: Context,
        hasCaseBattery: Boolean,
        caseLevel: Int = -1,
        isLeftCharging: Boolean = false,
        isRightCharging: Boolean = false,
        isCaseCharging: Boolean = false,
        width: Int = 320,
        height: Int = 144,
    ): Icon {
        return createChargingCase3DIcon(context, caseLevel)
    }
}
