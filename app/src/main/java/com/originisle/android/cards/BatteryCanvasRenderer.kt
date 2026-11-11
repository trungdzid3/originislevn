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
     * Vẽ chi tiết tai nghe in-ear Vivo TWS 3D trắng ngọc trai:
     * - isLeft = true (tai trái, nằm bên trái đảo): Cuống tai ở bên ngoài bên trái, củ tai vươn vào tâm đảo bên phải (không lật).
     * - isLeft = false (tai phải, nằm bên phải đảo): Lật quanh trục X để cuống tai ở bên ngoài bên phải, củ tai vươn vào tâm đảo bên trái.
     */
    fun drawInEarBud(
        canvas: Canvas,
        bounds: RectF,
        isLeft: Boolean,
        isCharging: Boolean = false,
    ) {
        val count = canvas.save()
        if (!isLeft) {
            // Tai PHẢI: Lật trục X quanh tâm bounds để cuống tai ở bên ngoài bên phải, củ tai vươn vào tâm đảo bên trái
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
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val padding = size * 0.06f
        val bounds = RectF(padding, padding, size - padding, size - padding)
        drawInEarBud(canvas, bounds, isLeft, isCharging = false)
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

        // Lớp 4 — Đèn LED trung tâm phát sáng rực rỡ với quầng sáng (halo glow) lớn
        val ledColor = when {
            isCharging -> Color.parseColor("#00E676")
            level in 0..15 -> Color.parseColor("#FF5247")
            level in 16..30 -> Color.parseColor("#FFB020")
            else -> Color.parseColor("#00D084")
        }

        val cx = 120f
        val cy = 142f

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

        canvas.restoreToCount(count)
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
}
