package valentia.util

fun Char.isLetterOrUndescore(): Boolean = isLetter() || this == '_'
fun Char.isLetterOrDigitOrUndescore(): Boolean = isLetter() || isDigit() || this == '_'
fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'
