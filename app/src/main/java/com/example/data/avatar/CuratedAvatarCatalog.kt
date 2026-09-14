package com.example.data.avatar

import android.content.Context
import android.net.Uri
import com.example.data.model.UserRole
import java.io.File

/**
 * Curated preset avatars tailored for Students and Teachers.
 * Can be selected instantly and cached locally.
 */
data class AvatarPreset(
    val id: String,
    val title: String,
    val description: String,
    val targetRole: UserRole?,
    val style: String,
    val prompt: String,
    val emoji: String,
    val accentHex: String
) {
    /**
     * Resolves a persistent local URI for this preset, generating and caching
     * the asset if it does not already exist on the device.
     */
    fun resolveAvatarUri(context: Context): String {
        val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
        val file = File(dir, "preset_${id}.png")
        if (file.exists() && file.length() > 0) {
            return Uri.fromFile(file).toString()
        }

        val role = targetRole ?: UserRole.STUDENT
        val bitmap = ProceduralAvatarGenerator.generateAvatarBitmap(
            prompt = prompt,
            style = style,
            role = role,
            seed = id.hashCode().toLong()
        )
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(file).toString()
    }
}

object CuratedAvatarCatalog {

    val STUDENT_AVATARS = listOf(
        AvatarPreset(
            id = "student_cyberpunk_scholar",
            title = "Cyberpunk Scholar",
            description = "High-tech student with glowing neon headset & holographic notes",
            targetRole = UserRole.STUDENT,
            style = "Cyberpunk Neon",
            prompt = "Cyberpunk neon tech student wearing cyan headphones and sleek hoodie",
            emoji = "⚡",
            accentHex = "#00F5FF"
        ),
        AvatarPreset(
            id = "student_cosmic_explorer",
            title = "Cosmic Explorer",
            description = "Astronomy prodigy charting constellations and planetary physics",
            targetRole = UserRole.STUDENT,
            style = "3D Pixar",
            prompt = "Young space cadet astronaut student with starry galactic helmet and notebook",
            emoji = "🚀",
            accentHex = "#6366F1"
        ),
        AvatarPreset(
            id = "student_anime_prodigy",
            title = "Anime Prodigy",
            description = "Dynamic high-achieving student studying with sharp focus",
            targetRole = UserRole.STUDENT,
            style = "Anime",
            prompt = "Anime manga smart student character studying with determination, cherry blossom aura",
            emoji = "🌸",
            accentHex = "#EC4899"
        ),
        AvatarPreset(
            id = "student_math_olympian",
            title = "Math Olympian",
            description = "Analytical thinker calculating complex geometric proofs",
            targetRole = UserRole.STUDENT,
            style = "Minimalist",
            prompt = "Smart mathematics student with golden ratio geometric symbols and sleek glasses",
            emoji = "📐",
            accentHex = "#F59E0B"
        ),
        AvatarPreset(
            id = "student_pixel_coder",
            title = "Retro 8-Bit Coder",
            description = "Algorithmic software engineer building pixelated universes",
            targetRole = UserRole.STUDENT,
            style = "Pixel Art",
            prompt = "8-bit retro pixel art student programmer hacker wearing cozy hoodie",
            emoji = "👾",
            accentHex = "#10B981"
        ),
        AvatarPreset(
            id = "student_bio_researcher",
            title = "Bio-Tech Explorer",
            description = "Curious scientist studying genetics and ecological microbiology",
            targetRole = UserRole.STUDENT,
            style = "Watercolor",
            prompt = "Gentle watercolor student researcher with lab coat and green leafy botanic aura",
            emoji = "🔬",
            accentHex = "#14B8A6"
        )
    )

    val TEACHER_AVATARS = listOf(
        AvatarPreset(
            id = "teacher_distinguished_prof",
            title = "Distinguished Professor",
            description = "Senior faculty leader in academic regalia with timeless wisdom",
            targetRole = UserRole.TEACHER,
            style = "Academic Oil Painting",
            prompt = "Distinguished professor wearing academic mortarboard cap and navy tie with scholarly glasses",
            emoji = "🎓",
            accentHex = "#F59E0B"
        ),
        AvatarPreset(
            id = "teacher_quantum_mentor",
            title = "Quantum Physics Mentor",
            description = "Passionate educator demonstrating particle physics and astrophysics",
            targetRole = UserRole.TEACHER,
            style = "Cyberpunk Neon",
            prompt = "Physics professor with laser spectacles and atomic orbital holographic display",
            emoji = "⚛️",
            accentHex = "#06B6D4"
        ),
        AvatarPreset(
            id = "teacher_literature_sage",
            title = "Literature & Arts Sage",
            description = "Inspiring humanities professor surrounded by classic literature",
            targetRole = UserRole.TEACHER,
            style = "Watercolor",
            prompt = "Warm literature and humanities teacher with warm glasses and poetic library backdrop",
            emoji = "📚",
            accentHex = "#8B5CF6"
        ),
        AvatarPreset(
            id = "teacher_robotics_dean",
            title = "Robotics & AI Dean",
            description = "Visionary engineering instructor leading artificial intelligence labs",
            targetRole = UserRole.TEACHER,
            style = "3D Pixar",
            prompt = "Modern computer science faculty teacher with sleek robotic assistant pin",
            emoji = "🤖",
            accentHex = "#3B82F6"
        ),
        AvatarPreset(
            id = "teacher_math_maestro",
            title = "Mathematics Maestro",
            description = "Master educator transforming complex calculus into clear insights",
            targetRole = UserRole.TEACHER,
            style = "Minimalist",
            prompt = "Professor of mathematics with golden spectacles and clean chalkboard geometry",
            emoji = "✨",
            accentHex = "#10B981"
        ),
        AvatarPreset(
            id = "teacher_history_chronicler",
            title = "History Chronicler",
            description = "Passionate historian guiding students through civilizations & eras",
            targetRole = UserRole.TEACHER,
            style = "3D Pixar",
            prompt = "World history educator holding vintage globe and ancient manuscripts",
            emoji = "🏛️",
            accentHex = "#D97706"
        )
    )

    fun getAllPresets(role: UserRole?): List<AvatarPreset> {
        return when (role) {
            UserRole.STUDENT -> STUDENT_AVATARS + TEACHER_AVATARS
            UserRole.TEACHER -> TEACHER_AVATARS + STUDENT_AVATARS
            else -> STUDENT_AVATARS + TEACHER_AVATARS
        }
    }
}
