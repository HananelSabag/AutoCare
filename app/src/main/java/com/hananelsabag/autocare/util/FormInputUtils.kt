package com.hananelsabag.autocare.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Displays a raw digit string with thousands-separator commas.
 * Example: "125000" → "125,000", "1234567" → "1,234,567"
 * The underlying value stays as digits-only; commas are visual-only.
 */
object ThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // Build formatted string with commas
        val formatted = buildString {
            original.forEachIndexed { i, c ->
                append(c)
                val digitsFromEnd = original.length - i - 1
                if (digitsFromEnd > 0 && digitsFromEnd % 3 == 0) append(',')
            }
        }

        // Count commas that appear before original[offset] in the formatted string
        fun commasBefore(originalOffset: Int): Int {
            var count = 0
            for (i in 0 until originalOffset) {
                val digitsFromEnd = original.length - i - 1
                if (digitsFromEnd > 0 && digitsFromEnd % 3 == 0) count++
            }
            return count
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset + commasBefore(offset)

            override fun transformedToOriginal(offset: Int): Int {
                // Walk the formatted string, counting only non-comma characters
                var origIdx = 0
                for (i in 0 until offset) {
                    if (i < formatted.length && formatted[i] != ',') origIdx++
                }
                return origIdx.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
