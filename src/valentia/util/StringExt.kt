package valentia.util

fun String.quoted(): String = buildString {
    append('"')
    for (c in this@quoted) {
        when (c) {
            '"', '\\' -> append('\\').append(c)
            '\n' -> append('\\').append('n')
            '\r' -> append('\\').append('r')
            '\t' -> append('\\').append('t')
            else -> append(c)
        }

    }
    append('"')
}