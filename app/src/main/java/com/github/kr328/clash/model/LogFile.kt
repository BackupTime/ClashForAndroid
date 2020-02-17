package com.github.kr328.clash.model

data class LogFile(val fileName: String, val date: Long) {
    companion object {
        private val REGEX_FILE = Regex("clash-(\\d+).log")
        private const val FORMAT_FILE_NAME = "clash-%d.log"

        fun parseFromFileName(fileName: String): LogFile? {
            return REGEX_FILE.matchEntire(fileName)?.run {
                LogFile(fileName, groupValues[1].toLong())
            }
        }

        fun generate(date: Long = System.currentTimeMillis()): LogFile {
            val fileName = FORMAT_FILE_NAME.format(date)

            return LogFile(fileName, date)
        }
    }
}