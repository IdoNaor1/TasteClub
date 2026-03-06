package com.tasteclub.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converts a Unix-millisecond timestamp to a human-readable relative string.
 *
 * Examples:
 *   < 60 s   → "just now"
 *   < 60 min → "5m ago"
 *   < 24 h   → "3h ago"
 *   < 7 d    → "2d ago"
 *   otherwise → "Mar 5, 2026"
 */
fun Long.toRelativeString(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this))
    }
}

