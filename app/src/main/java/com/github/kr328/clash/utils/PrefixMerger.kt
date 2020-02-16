package com.github.kr328.clash.utils

import com.github.kr328.clash.core.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrefixMerger {
    private val REGEX_PREFIX_TRIM = Regex("[-]*$")

    data class Result<T>(val prefix: String, val content: String, val value: T)

    suspend fun <T> merge(values: List<T>, transform: (T) -> String): List<Result<T>> =
        withContext(Dispatchers.Default) {
            val pairs = values.map {
                transform(it).trim() to it
            }

            val groups = mutableListOf<List<Pair<String, T>>>()
            var mergingGroup = mutableListOf<Pair<String, T>>()
            var currentChar: Char = 0.toChar()
            val result = mutableListOf<Result<T>>()

            for (pair in pairs) {
                if (pair.first[0] == currentChar) {
                    mergingGroup.add(pair)
                } else {
                    if (mergingGroup.isNotEmpty()) {
                        groups.add(mergingGroup)
                        mergingGroup = mutableListOf()
                    }

                    currentChar = pair.first[0]
                    mergingGroup.add(pair)
                }
            }

            if ( mergingGroup.isNotEmpty() )
                groups.add(mergingGroup)

            for (group in groups) {
                var diffIndex = 0

                diff@ for (charIndex in group[0].first.indices) {
                    for (stringIndex in 0 until (group.size - 1)) {
                        if (group[stringIndex].first[charIndex] != group[stringIndex + 1].first[charIndex])
                            break@diff
                    }

                    diffIndex++
                }

                group.forEach {
                    result.add(
                        Result(
                            it.first.substring(0, diffIndex)
                                .replace(REGEX_PREFIX_TRIM, ""),
                            it.first.substring(diffIndex),
                            it.second
                        )
                    )
                }
            }

            result
        }
}