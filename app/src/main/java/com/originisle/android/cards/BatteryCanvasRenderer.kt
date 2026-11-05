package com.originisle.android.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
}
