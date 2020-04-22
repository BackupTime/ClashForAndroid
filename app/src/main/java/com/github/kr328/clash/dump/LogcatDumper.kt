package com.github.kr328.clash.dump

object LogcatDumper {
    fun dump(): String {
        return try {
            val process =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "-v", "raw", "Go", "AndroidRuntime", "DEBUG"))

            val result = process.inputStream.use {
                it.reader().readText()
            }

            process.waitFor()

            result
        } catch (e: Exception) {
            ""
        }
    }
}
