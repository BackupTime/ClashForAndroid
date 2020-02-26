package com.github.kr328.clash.utils

fun String.toCodePointList(): List<Int> {
    var offset = 0
    val result = mutableListOf<Int>()

    while (offset < length) {
        val codePoint = codePointAt(offset)
        result.add(codePoint)

        offset += Character.charCount(codePoint)
    }

    return result.toList()
}

fun List<Int>.asCodePointString(): String {
    val sb = StringBuilder()

    forEach {
        sb.appendCodePoint(it)
    }

    return sb.toString()
}