package com.logicalsapien.sapienterm.util

/**
 * Detects when a single terminal screen line is part of the shell echo of a large base64 image
 * sent from the floating input dialog. The remote echoes the same bytes; the terminal wraps
 * across rows, so [lineText] is usually a substring of the payload, not the full string.
 */
fun lineEchoMatchesFloatingImagePayload(lineText: String, payload: String): Boolean {
    val t = lineText.trimEnd('\r', '\n')
    if (t.isEmpty() || payload.length < 100) return false

    if (payload == t) return true

    if (t.length >= 32 && payload.contains(t)) return true

    val suffix = base64SuffixFromLine(t)
    if (suffix.isEmpty()) return false
    if (!payload.contains(suffix)) return false
    if (suffix.length >= 16) return true
    return payload.endsWith(suffix)
}

/**
 * Longest suffix of [line] consisting only of base64 alphabet characters (after trim).
 * Skips shell prompt / noise before the echoed base64 on the first line.
 */
fun base64SuffixFromLine(line: String): String {
    val t = line.trimEnd('\r', '\n')
    if (t.isEmpty()) return ""
    var i = t.length - 1
    while (i >= 0 && (t[i].isLetterOrDigit() || t[i] in "+/=")) i--
    return t.substring(i + 1)
}
