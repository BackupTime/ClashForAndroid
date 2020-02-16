package com.github.kr328.clash.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UiPreferences(context: Context) {
    companion object {
        private const val FILE_NAME = "ui"

        const val PROXY_SORT_DEFAULT = "default"
        const val PROXY_SORT_NAME = "name"
        const val PROXY_SORT_DELAY = "delay"

        val PROXY_GROUP_SORT = StringEntry("proxy_group_sort", PROXY_SORT_DEFAULT)
        val PROXY_PROXY_SORT = StringEntry("proxy_proxy_sort", PROXY_SORT_DEFAULT)
        val PROXY_LAST_SELECT_GROUP = StringEntry("proxy_last_select_group", "")
        val PROXY_MERGE_PREFIX = BooleanEntry("proxy_merge_prefix", true)
    }

    interface Entry<T> {
        fun get(sharedPreferences: SharedPreferences): T
        fun put(editor: SharedPreferences.Editor, value: T)
    }

    class StringEntry(private val key: String, private val defaultValue: String? = null) :
        Entry<String> {
        override fun get(sharedPreferences: SharedPreferences): String {
            return sharedPreferences.getString(key, defaultValue)!!
        }

        override fun put(editor: SharedPreferences.Editor, value: String) {
            editor.putString(key, value)
        }
    }

    class BooleanEntry(private val key: String, private val defaultValue: Boolean = false):
            Entry<Boolean> {
        override fun get(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean(key, defaultValue)
        }

        override fun put(editor: SharedPreferences.Editor, value: Boolean) {
            editor.putBoolean(key, value)
        }
    }

    class Editor(private val editor: SharedPreferences.Editor) {
        fun <T, E : Entry<T>> put(e: E, value: T) {
            e.put(editor, value)
        }
    }

    private val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun <T, E : Entry<T>> get(e: E): T {
        return e.get(sharedPreferences)
    }

    fun edit(block: Editor.() -> Unit) {
        val editor = sharedPreferences.edit()

        Editor(editor).apply(block)

        editor.apply()
    }
}