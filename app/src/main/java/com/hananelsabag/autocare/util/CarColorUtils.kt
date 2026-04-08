package com.hananelsabag.autocare.util

import androidx.compose.ui.graphics.Color

/**
 * Maps a car color name (Hebrew or English) to a Compose [Color].
 * Returns null for unrecognized color names — callers should fall back to a theme token.
 */
fun carColorToComposeColor(colorName: String): Color? = when (colorName.lowercase().trim()) {
    "לבן", "white"              -> Color.White
    "שחור", "black"             -> Color(0xFF1A1A1A)
    "אפור", "grey", "gray"      -> Color(0xFF757575)
    "כסוף", "silver"            -> Color(0xFFC0C0C0)
    "אדום", "red"               -> Color(0xFFE53935)
    "כחול", "blue"              -> Color(0xFF1E88E5)
    "ירוק", "green"             -> Color(0xFF43A047)
    "צהוב", "yellow"            -> Color(0xFFFDD835)
    "כתום", "orange"            -> Color(0xFFFF6D00)
    "חום", "brown"              -> Color(0xFF6D4C41)
    "סגול", "purple"            -> Color(0xFF8E24AA)
    "ורוד", "pink"              -> Color(0xFFE91E63)
    "בז'", "בז", "beige"        -> Color(0xFFF5F0DC)
    "זהב", "gold"               -> Color(0xFFFFCA28)
    "נייבי", "navy"             -> Color(0xFF1A237E)
    else                        -> null
}
