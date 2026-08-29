package com.athr.karaoketv.util

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

fun formatCount(n: Int): String = when {
    n >= 1000 -> "%,d".format(n)
    else -> n.toString()
}
