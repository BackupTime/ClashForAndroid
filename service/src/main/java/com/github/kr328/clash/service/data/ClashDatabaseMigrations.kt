package com.github.kr328.clash.service.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.Constants
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import java.io.File

object ClashDatabaseMigrations {
    val VERSION_1_2 = object : Migration(1, 2) {
        private fun process(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE profiles RENAME TO _profiles")
            database.execSQL("ALTER TABLE profile_select_proxies RENAME TO _profile_select_proxies")

            database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `uri` TEXT NOT NULL, `source` TEXT, `active` INTEGER NOT NULL, `last_update` INTEGER NOT NULL, `update_interval` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `profile_select_proxies` (`profile_id` INTEGER NOT NULL, `proxy` TEXT NOT NULL, `selected` TEXT NOT NULL, PRIMARY KEY(`profile_id`, `proxy`), FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")

            try {
                val cursor =
                    database.query("SELECT name, token, file, active, last_update, id FROM _profiles")

                Global.application.filesDir.resolve(Constants.CLASH_DIR).listFiles()?.forEach {
                    it.deleteRecursively()
                }

                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    // old
                    // name, token, file, active, last_update, id
                    val name = cursor.getString(0)
                    val token = cursor.getString(1)
                    val file = cursor.getString(2)
                    val active = cursor.getInt(3)
                    val lastUpdate = cursor.getLong(4)
                    val id = cursor.getLong(5)

                    // new
                    // name, type, uri, source, active, last_update, update_interval, id
                    val type = when {
                        token.startsWith("url") -> ClashProfileEntity.TYPE_URL
                        token.startsWith("file") -> ClashProfileEntity.TYPE_FILE
                        else -> ClashProfileEntity.TYPE_UNKNOWN
                    }

                    File(file).renameTo(resolveProfile(id))
                    resolveBase(id).mkdirs()

                    database.insert("profiles",
                        SQLiteDatabase.CONFLICT_ABORT,
                        ContentValues().apply {
                            put("name", name)
                            put("type", type)
                            put("uri", token.removePrefix("url|").removePrefix("file|"))
                            putNull("source")
                            put("active", active)
                            put("last_update", lastUpdate)
                            put("update_interval", 0)
                            put("id", id)
                        })

                    cursor.moveToNext()
                }
                cursor.close()
            }
            catch (e: Exception) {
                Log.d("Migration profiles failure", e)
            }

            try {
                val cursor = database.query("SELECT profile_id, proxy, selected FROM _profile_select_proxies ORDER BY id")

                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    // old
                    // profile_id, proxy, selected, id
                    val profileId = cursor.getLong(0)
                    val proxy: String = cursor.getString(1)
                    val selected = cursor.getString(2)

                    // new
                    // profile_id, proxy, selected

                    database.insert("profile_select_proxies",
                        SQLiteDatabase.CONFLICT_REPLACE,
                        ContentValues().apply {
                            put("profile_id", profileId)
                            put("proxy", proxy)
                            put("selected", selected)
                        })

                    cursor.moveToNext()
                }
                cursor.close()
            }
            catch (e: Exception) {
                Log.d("Migration selected failure")
            }

            database.execSQL("DROP TABLE _profiles")
            database.execSQL("DROP TABLE _profile_select_proxies")
        }

        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                process(database)
                Log.i("Database Migrated 1 -> 2")
            }
            catch (e: Exception) {
                Log.e("Migration failure", e)
            }
        }
    }
}