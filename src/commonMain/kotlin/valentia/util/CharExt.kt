package valentia.util

fun Char.isLetterOrDigitOrUndescore(): Boolean = isLetter() || isDigit() || this == '_'
