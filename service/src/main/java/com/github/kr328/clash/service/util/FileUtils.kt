package com.github.kr328.clash.service.util

import java.io.File
import java.security.SecureRandom
import kotlin.math.absoluteValue

object FileUtils {
    private val random = SecureRandom()

    fun generateRandomFileName(dir: File, suffix: String = ""): String {
        dir.mkdirs()

        var fileName: String

        do {
            fileName = random.nextLong().absoluteValue.toString() + suffix
        } while (dir.resolve(fileName).exists())

        return fileName
    }
}