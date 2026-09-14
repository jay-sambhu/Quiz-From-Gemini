package com.example.data.avatar

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.example.data.model.UserRole
import java.io.File
import kotlin.math.*
import kotlin.random.Random

/**
 * High-resolution procedural vector/canvas avatar generator.
 * Creates clean, distinctive, artistic 512x512 circular profile avatars
 * tailored to Student and Teacher roles, requested styles, and prompt keywords.
 */
object ProceduralAvatarGenerator {

    private const val AVATAR_SIZE = 512

    fun generateAvatarBitmap(
        prompt: String,
        style: String,
        role: UserRole,
        seed: Long = prompt.hashCode().toLong() + style.hashCode().toLong()
    ): Bitmap {
        val random = Random(seed)
        val bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val promptLower = prompt.lowercase()
        val styleLower = style.lowercase()

        // 1. Determine Color Palette based on style & keywords
        val palette = resolvePalette(promptLower, styleLower, role, random)

        // 2. Draw Background & Vignette
        drawBackground(canvas, palette, styleLower, random)

        // 3. Draw Ambient Halo / Aura
        drawAuraOrRing(canvas, palette, styleLower)

        // 4. Draw Character Torso / Clothing
        drawTorso(canvas, palette, role, promptLower, styleLower)

        // 5. Draw Head, Face & Hair
        drawHeadAndFace(canvas, palette, styleLower, random)

        // 6. Draw Accessories (Headphones, Glasses, Academic Cap, Goggles, Necktie)
        drawAccessories(canvas, palette, role, promptLower, styleLower, random)

        // 7. Draw Artistic Sparkles & AI Particle Accents
        drawAtmosphericAccents(canvas, palette, styleLower, random)

        // 8. Draw Border Ring / Outer Mask
        drawBorderRing(canvas, palette)

        return bitmap
    }

    /**
     * Saves the generated avatar bitmap to the app's internal profile photos directory.
     * Returns the persistent file URI string.
     */
    fun saveAvatarToFile(context: Context, bitmap: Bitmap, prefix: String = "ai_avatar"): String {
        val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
        val filename = "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.png"
        val file = File(dir, filename)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(file).toString()
    }

    private data class AvatarPalette(
        val bgStart: Int,
        val bgEnd: Int,
        val accentColor: Int,
        val secondaryColor: Int,
        val skinColor: Int,
        val hairColor: Int,
        val clothingColor: Int,
        val rimColor: Int
    )

    private fun resolvePalette(
        prompt: String,
        artStyle: String,
        role: UserRole,
        random: Random
    ): AvatarPalette {
        return when {
            artStyle.contains("cyberpunk") || prompt.contains("neon") || prompt.contains("cyber") -> {
                AvatarPalette(
                    bgStart = Color.rgb(15, 12, 41),
                    bgEnd = Color.rgb(48, 43, 99),
                    accentColor = Color.rgb(0, 245, 255), // Cyan neon
                    secondaryColor = Color.rgb(255, 0, 128), // Magenta neon
                    skinColor = Color.rgb(240, 200, 180),
                    hairColor = Color.rgb(0, 220, 255),
                    clothingColor = Color.rgb(28, 28, 48),
                    rimColor = Color.rgb(0, 255, 200)
                )
            }
            artStyle.contains("anime") || prompt.contains("anime") || prompt.contains("manga") -> {
                AvatarPalette(
                    bgStart = Color.rgb(255, 175, 189),
                    bgEnd = Color.rgb(255, 195, 160),
                    accentColor = Color.rgb(255, 75, 140),
                    secondaryColor = Color.rgb(108, 92, 231),
                    skinColor = Color.rgb(255, 224, 206),
                    hairColor = if (random.nextBoolean()) Color.rgb(74, 105, 189) else Color.rgb(235, 94, 40),
                    clothingColor = Color.rgb(60, 64, 198),
                    rimColor = Color.rgb(255, 255, 255)
                )
            }
            artStyle.contains("pixel") || prompt.contains("pixel") -> {
                AvatarPalette(
                    bgStart = Color.rgb(33, 150, 243),
                    bgEnd = Color.rgb(13, 71, 161),
                    accentColor = Color.rgb(255, 215, 0),
                    secondaryColor = Color.rgb(76, 175, 80),
                    skinColor = Color.rgb(255, 213, 170),
                    hairColor = Color.rgb(62, 39, 35),
                    clothingColor = Color.rgb(244, 67, 54),
                    rimColor = Color.rgb(255, 235, 59)
                )
            }
            artStyle.contains("watercolor") -> {
                AvatarPalette(
                    bgStart = Color.rgb(224, 242, 241),
                    bgEnd = Color.rgb(178, 223, 219),
                    accentColor = Color.rgb(0, 150, 136),
                    secondaryColor = Color.rgb(255, 138, 101),
                    skinColor = Color.rgb(255, 230, 215),
                    hairColor = Color.rgb(109, 76, 65),
                    clothingColor = Color.rgb(77, 182, 172),
                    rimColor = Color.rgb(255, 255, 255)
                )
            }
            role == UserRole.TEACHER || prompt.contains("professor") || prompt.contains("teacher") -> {
                AvatarPalette(
                    bgStart = Color.rgb(30, 41, 59),
                    bgEnd = Color.rgb(15, 23, 42),
                    accentColor = Color.rgb(245, 158, 11), // Academic Amber
                    secondaryColor = Color.rgb(99, 102, 241), // Royal Indigo
                    skinColor = Color.rgb(245, 215, 190),
                    hairColor = if (random.nextBoolean()) Color.rgb(148, 163, 184) else Color.rgb(71, 85, 105),
                    clothingColor = Color.rgb(30, 58, 138), // Navy professor suit
                    rimColor = Color.rgb(251, 191, 36)
                )
            }
            else -> {
                // Student & 3D Vibrant Default
                AvatarPalette(
                    bgStart = Color.rgb(99, 102, 241), // Bento Violet
                    bgEnd = Color.rgb(67, 56, 202),
                    accentColor = Color.rgb(16, 185, 129), // Emerald
                    secondaryColor = Color.rgb(244, 63, 94), // Rose
                    skinColor = Color.rgb(255, 219, 187),
                    hairColor = Color.rgb(45, 52, 54),
                    clothingColor = Color.rgb(238, 90, 36),
                    rimColor = Color.rgb(255, 255, 255)
                )
            }
        }
    }

    private fun drawBackground(canvas: Canvas, palette: AvatarPalette, artStyle: String, random: Random) {
        val center = AVATAR_SIZE / 2f
        val radius = AVATAR_SIZE / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                center, center * 0.75f,
                radius * 1.1f,
                palette.bgStart,
                palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, radius, paint)

        // Stylized background pattern
        if (artStyle.contains("cyberpunk") || artStyle.contains("tech")) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
                alpha = 35
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            for (i in 40 until AVATAR_SIZE step 40) {
                canvas.drawLine(0f, i.toFloat(), AVATAR_SIZE.toFloat(), i.toFloat(), gridPaint)
                canvas.drawLine(i.toFloat(), 0f, i.toFloat(), AVATAR_SIZE.toFloat(), gridPaint)
            }
        } else if (artStyle.contains("pixel")) {
            val dotPaint = Paint().apply {
                color = palette.accentColor
                alpha = 40
            }
            for (x in 20 until AVATAR_SIZE step 28) {
                for (y in 20 until AVATAR_SIZE step 28) {
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + 8).toFloat(), (y + 8).toFloat(), dotPaint)
                }
            }
        }
    }

    private fun drawAuraOrRing(canvas: Canvas, palette: AvatarPalette, artStyle: String) {
        val center = AVATAR_SIZE / 2f
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = palette.accentColor
            alpha = 120
        }
        canvas.drawCircle(center, center, center - 24f, auraPaint)

        val auraSoftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 18f
            color = palette.secondaryColor
            alpha = 60
        }
        canvas.drawCircle(center, center, center - 32f, auraSoftPaint)
    }

    private fun drawTorso(
        canvas: Canvas,
        palette: AvatarPalette,
        role: UserRole,
        prompt: String,
        artStyle: String
    ) {
        val torsoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.clothingColor
            style = Paint.Style.FILL
        }

        val path = Path().apply {
            moveTo(110f, 512f)
            cubicTo(120f, 380f, 180f, 340f, 256f, 340f)
            cubicTo(332f, 340f, 392f, 380f, 402f, 512f)
            close()
        }
        canvas.drawPath(path, torsoPaint)

        // Collar / Shirt / Hoodie Accent
        val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        if (role == UserRole.TEACHER || prompt.contains("professor") || prompt.contains("teacher")) {
            // Teacher Collar & Tie
            val collarPath = Path().apply {
                moveTo(220f, 340f)
                lineTo(256f, 390f)
                lineTo(292f, 340f)
                close()
            }
            canvas.drawPath(collarPath, collarPaint)

            // Tie
            val tiePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.secondaryColor
                style = Paint.Style.FILL
            }
            val tiePath = Path().apply {
                moveTo(250f, 390f)
                lineTo(262f, 390f)
                lineTo(268f, 480f)
                lineTo(256f, 500f)
                lineTo(244f, 480f)
                close()
            }
            canvas.drawPath(tiePath, tiePaint)
        } else {
            // Student Hoodie strings / collar
            val hoodieLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
                strokeWidth = 5f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(240f, 350f, 235f, 440f, hoodieLinePaint)
            canvas.drawLine(272f, 350f, 277f, 440f, hoodieLinePaint)
        }
    }

    private fun drawHeadAndFace(
        canvas: Canvas,
        palette: AvatarPalette,
        artStyle: String,
        random: Random
    ) {
        val center = AVATAR_SIZE / 2f
        val headRadius = 90f
        val headCenterY = 240f

        // Head shadow/neck
        val neckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustColorBrightness(palette.skinColor, 0.85f)
        }
        canvas.drawRoundRect(RectF(230f, 300f, 282f, 360f), 12f, 12f, neckPaint)

        // Face Base
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.skinColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, headCenterY, headRadius, facePaint)

        // Hair (Back & Front volume)
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.hairColor
            style = Paint.Style.FILL
        }

        // Hair Cap
        val hairPath = Path().apply {
            moveTo(center - headRadius - 6f, headCenterY - 10f)
            cubicTo(
                center - headRadius - 10f, headCenterY - 105f,
                center + headRadius + 10f, headCenterY - 105f,
                center + headRadius + 6f, headCenterY - 10f
            )
            cubicTo(
                center + headRadius, headCenterY - 45f,
                center - headRadius, headCenterY - 45f,
                center - headRadius - 6f, headCenterY - 10f
            )
            close()
        }
        canvas.drawPath(hairPath, hairPaint)

        // Eyes
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 40)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center - 32f, headCenterY + 5f, 9f, eyePaint)
        canvas.drawCircle(center + 32f, headCenterY + 5f, 9f, eyePaint)

        // Eye Catchlight Sparkle
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(center - 34f, headCenterY + 2f, 3.5f, sparklePaint)
        canvas.drawCircle(center + 30f, headCenterY + 2f, 3.5f, sparklePaint)

        // Eyebrows
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.hairColor
            strokeWidth = 4.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(center - 44f, headCenterY - 18f, center - 20f, headCenterY - 15f, browPaint)
        canvas.drawLine(center + 20f, headCenterY - 15f, center + 44f, headCenterY - 18f, browPaint)

        // Smile
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 70, 70)
            strokeWidth = 4.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val mouthRect = RectF(center - 20f, headCenterY + 28f, center + 20f, headCenterY + 48f)
        canvas.drawArc(mouthRect, 10f, 160f, false, mouthPaint)

        // Cheeks Blush
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 120, 140)
            alpha = 75
        }
        canvas.drawCircle(center - 48f, headCenterY + 22f, 12f, blushPaint)
        canvas.drawCircle(center + 48f, headCenterY + 22f, 12f, blushPaint)
    }

    private fun drawAccessories(
        canvas: Canvas,
        palette: AvatarPalette,
        role: UserRole,
        prompt: String,
        artStyle: String,
        random: Random
    ) {
        val center = AVATAR_SIZE / 2f
        val headCenterY = 240f

        // Glasses (Academic, Professor, Smart Student, or Cyberpunk)
        val wearsGlasses = role == UserRole.TEACHER ||
                prompt.contains("glasses") ||
                prompt.contains("professor") ||
                prompt.contains("smart") ||
                prompt.contains("nerd") ||
                artStyle.contains("cyberpunk")

        if (wearsGlasses) {
            val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (artStyle.contains("cyberpunk")) palette.accentColor else Color.rgb(220, 180, 60)
                strokeWidth = if (artStyle.contains("cyberpunk")) 5.5f else 4f
                style = Paint.Style.STROKE
            }
            // Left rim
            canvas.drawRoundRect(RectF(center - 54f, headCenterY - 14f, center - 10f, headCenterY + 22f), 10f, 10f, glassPaint)
            // Right rim
            canvas.drawRoundRect(RectF(center + 10f, headCenterY - 14f, center + 54f, headCenterY + 22f), 10f, 10f, glassPaint)
            // Bridge
            canvas.drawLine(center - 10f, headCenterY + 2f, center + 10f, headCenterY + 2f, glassPaint)
        }

        // Headphones (Student / Tech / Cyber / Coder)
        val wearsHeadphones = (role == UserRole.STUDENT || prompt.contains("headphone") || prompt.contains("music") || prompt.contains("gamer") || prompt.contains("coder")) && !wearsGlasses
        if (wearsHeadphones) {
            val hpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
                strokeWidth = 10f
                style = Paint.Style.STROKE
            }
            val hpRect = RectF(center - 105f, headCenterY - 90f, center + 105f, headCenterY + 80f)
            canvas.drawArc(hpRect, 180f, 180f, false, hpPaint)

            val earpiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.secondaryColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(center - 116f, headCenterY - 15f, center - 94f, headCenterY + 45f), 12f, 12f, earpiecePaint)
            canvas.drawRoundRect(RectF(center + 94f, headCenterY - 15f, center + 116f, headCenterY + 45f), 12f, 12f, earpiecePaint)
        }

        // Mortarboard Cap (Teacher, Graduate, Academic prompt)
        val wearsMortarboard = prompt.contains("graduate") || prompt.contains("academic") || (role == UserRole.TEACHER && prompt.contains("dean"))
        if (wearsMortarboard) {
            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(24, 24, 38)
                style = Paint.Style.FILL
            }
            val capPath = Path().apply {
                moveTo(center, headCenterY - 120f)
                lineTo(center + 110f, headCenterY - 75f)
                lineTo(center, headCenterY - 30f)
                lineTo(center - 110f, headCenterY - 75f)
                close()
            }
            canvas.drawPath(capPath, capPaint)

            // Tassel
            val tasselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(center, headCenterY - 75f, center + 85f, headCenterY - 45f, tasselPaint)
            canvas.drawCircle(center + 85f, headCenterY - 40f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accentColor })
        }
    }

    private fun drawAtmosphericAccents(
        canvas: Canvas,
        palette: AvatarPalette,
        artStyle: String,
        random: Random
    ) {
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentColor
            style = Paint.Style.FILL
        }

        // Draw 4-point sparkles in background
        drawSparkle(canvas, sparklePaint, 90f, 110f, 14f)
        drawSparkle(canvas, sparklePaint, 410f, 130f, 18f)
        drawSparkle(canvas, sparklePaint, 430f, 380f, 12f)
        drawSparkle(canvas, sparklePaint, 75f, 360f, 10f)

        // AI Badge tag in lower right
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 15, 23, 42)
            style = Paint.Style.FILL
        }
        val badgeRect = RectF(340f, 430f, 470f, 470f)
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBgPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ AI AVATAR", 405f, 456f, badgeTextPaint)
    }

    private fun drawSparkle(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx, cy, cx + size, cy)
            quadTo(cx, cy, cx, cy + size)
            quadTo(cx, cy, cx - size, cy)
            quadTo(cx, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBorderRing(canvas: Canvas, palette: AvatarPalette) {
        val center = AVATAR_SIZE / 2f
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.rimColor
            alpha = 180
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(center, center, (AVATAR_SIZE / 2f) - 3f, borderPaint)
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).coerceIn(0f, 255f).toInt()
        val g = (Color.green(color) * factor).coerceIn(0f, 255f).toInt()
        val b = (Color.blue(color) * factor).coerceIn(0f, 255f).toInt()
        return Color.argb(a, r, g, b)
    }
}
