package com.example.ui.theme

/**
 * Global application theme mode choices.
 */
enum class AppThemeMode(val displayName: String, val description: String) {
    SYSTEM(
        displayName = "System Default",
        description = "Matches your Android device system appearance"
    ),
    LIGHT(
        displayName = "Light Mode",
        description = "Bright, high-contrast Bento canvas"
    ),
    DARK(
        displayName = "Dark Mode",
        description = "Deep slate palette optimized for eye comfort & low light"
    );

    companion object {
        fun fromString(value: String?): AppThemeMode {
            return when (value?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}
