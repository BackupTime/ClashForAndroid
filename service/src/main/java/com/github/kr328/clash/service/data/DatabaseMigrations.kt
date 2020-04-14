package com.github.kr328.clash.service.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.service.Constants
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.resolveBaseDir
import com.github.kr328.clash.service.util.resolveProfileFile
import java.io.File

object DatabaseMigrations {
    val VERSION_1_2 = object : Migration(1, 2) {
        private fun process(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE profiles RENAME TO _profiles")
            database.execSQL("ALTER TABLE profile_select_proxies RENAME TO _profile_select_proxies")

            database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `uri` TEXT NOT NULL, `source` TEXT, `active` INTEGER NOT NULL, `last_update` INTEGER NOT NULL, `update_interval` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `profile_select_proxies` (`profile_id` INTEGER NOT NULL, `proxy` TEXT NOT NULL, `selected` TEXT NOT NULL, PRIMARY KEY(`profile_id`, `proxy`), FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")

            database.query("SELECT name, token, file, active, last_update, id FROM _profiles")
                .use { cursor ->

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
                            token.startsWith("url") -> ProfileEntity.TYPE_URL
                            token.startsWith("file") -> ProfileEntity.TYPE_FILE
                            else -> ProfileEntity.TYPE_UNKNOWN
                        }

                        File(file).renameTo(Global.application.resolveProfileFile(id))
                        Global.application.resolveBaseDir(id).mkdirs()

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
                }

            database.query("SELECT profile_id, proxy, selected FROM _profile_select_proxies ORDER BY id")
                .use { cursor ->
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
                }

            database.execSQL("DROP TABLE _profiles")
            database.execSQL("DROP TABLE _profile_select_proxies")

            // Migration settings
            val oldSettings = Global.application
                .getSharedPreferences("clash_service", Context.MODE_PRIVATE)
            val newSettings = ServiceSettings(
                Global.application
                    .getSharedPreferences(Constants.SERVICE_SETTING_FILE_NAME, Context.MODE_PRIVATE)
            )

            val accessMode = oldSettings
                .getInt("key_access_control_mode", 0)
            val accessPackages = oldSettings
                .getStringSet("ley_access_control_apps", emptySet())!! // just typo :)
            val dnsHijack = oldSettings
                .getBoolean("key_dns_hijacking_enabled", true)
            val bypassPrivate = oldSettings
                .getBoolean("key_bypass_private_network", true)

            oldSettings.edit {
                clear()
            }

            newSettings.commit {
                val newAccessMode = when (accessMode) {
                    0 -> ServiceSettings.ACCESS_CONTROL_MODE_ALL
                    1 -> ServiceSettings.ACCESS_CONTROL_MODE_WHITELIST
                    2 -> ServiceSettings.ACCESS_CONTROL_MODE_BLACKLIST
                    else -> ServiceSettings.ACCESS_CONTROL_MODE_ALL
                }

                put(ServiceSettings.ACCESS_CONTROL_MODE, newAccessMode)
                put(ServiceSettings.ACCESS_CONTROL_PACKAGES, accessPackages)
                put(ServiceSettings.DNS_HIJACKING, dnsHijack)
                put(ServiceSettings.BYPASS_PRIVATE_NETWORK, bypassPrivate)
            }
        }

        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                process(database)
                Log.i("Database Migrated 1 -> 2")
            } catch (e: Exception) {
                Log.e("Migration failure", e)
            }
        }
    }

    val VERSION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE profile_select_proxies RENAME TO selected_proxies")
                database.execSQL("ALTER TABLE profiles RENAME TO _profiles")
                database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `uri` TEXT NOT NULL, `source` TEXT, `active` INTEGER NOT NULL, `interval` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                database.query("SELECT name, type, uri, source, active, update_interval, id FROM _profiles")
                    .use { cursor ->
                        cursor.moveToFirst()
                        while (!cursor.isAfterLast) {
                            // old
                            // name, type, uri, source, active, last_update, update_interval(seconds), id
                            // new
                            // name, type, uri, source, active, interval(millis seconds), id
                            val name = cursor.getString(0)
                            val type = cursor.getInt(1)
                            val uri = cursor.getString(2)
                            val source = cursor.getStringOrNull(3)
                            val active = cursor.getInt(4)
                            val interval = cursor.getInt(5)
                            val id = cursor.getLong(6)

                            database.insert("profiles",
                                SQLiteDatabase.CONFLICT_ABORT,
                                ContentValues().apply {
                                    put("name", name)
                                    put("type", type)
                                    put("uri", uri)
                                    put("source", source)
                                    put("active", active)
                                    put("interval", interval * 1000)
                                    put("id", id)
                                })

                            cursor.moveToNext()
                        }
                    }

                database.execSQL("DROP TABLE _profiles")

                val uiSp = Global.application
                    .getSharedPreferences("ui", Context.MODE_PRIVATE)
                val srvSp = Global.application
                    .getSharedPreferences("service", Context.MODE_PRIVATE)

                srvSp.edit {
                    putBoolean("enable_vpn", uiSp.getBoolean("enable_vpn", true))
                }

                NotificationManagerCompat.from(Global.application).apply {
                    deleteNotificationChannel("clash_status_channel")
                    deleteNotificationChannel("profile_service_status")
                    deleteNotificationChannel("profile_service_result")
                }

                Log.i("Database Migrated 2 -> 3")
            } catch (e: Exception) {
                Log.e("Migration failure", e)
            }
        }
    }
}