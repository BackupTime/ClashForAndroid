package com.github.kr328.clash.common.utils

data class PrefixTree<T>(val separator: String) {
    companion object {
        const val WILDCARD = "*"
    }

    private data class Node<T>(val children: MutableMap<String, Node<T>>, var data: T?)
    private val root = Node<T>(mutableMapOf(), null)

    fun insert(path: String, data: T) {
        val segment = path.split(separator).filter(String::isNotBlank)
        var current = root

        for ( s in segment ) {
            current = current.children.getOrPut(s) {
                Node(mutableMapOf(), null)
            }
        }

        current.data = data
    }

    fun find(path: String): T? {
        val segment = path.split(separator).filter(String::isNotBlank)
        var current = root

        for (s in segment) {
            current = when {
                current.children.containsKey(s) ->
                    current.children[s]!!
                current.children.containsKey(WILDCARD) ->
                    current.children[WILDCARD]!!
                else -> return null
            }
        }

        return current.data
    }
}