package com.github.kr328.clash.service.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.SecureRandom
import kotlin.math.absoluteValue

object ClashDatabaseMigrations {
    val VERSION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.beginTransaction()

            database.execSQL("ALTER TABLE profiles RENAME TO _profiles")

            database.execSQL(
                "CREATE TABLE profiles(" +
                        "name TEXT NOT NULL, " +
                        "type INTEGER NOT NULL, " +
                        "uri TEXT NOT NULL, " +
                        "file TEXT NOT NULL, " +
                        "base TEXT NOT NULL" +
                        "active INTEGER NOT NULL, " +
                        "last_update INTEGER NOT NULL, " +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
            )

            val cursor =
                database.query("SELECT name, token, file, active, last_update FROM _profile")
            val random = SecureRandom()
            val bases = mutableSetOf<Long>()

            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                // old
                val name = cursor.getString(0)
                val token = cursor.getString(1)
                val file = cursor.getString(2)
                val active = cursor.getInt(3)
                val lastUpdate = cursor.getLong(4)

                // new
                val type = when {
                    token.startsWith("url") -> ClashProfileEntity.TYPE_REMOTE
                    token.startsWith("file") -> ClashProfileEntity.TYPE_LOCAL
                    else -> ClashProfileEntity.TYPE_UNKNOWN
                }
                val uri = token.removePrefix("url|").removePrefix("file|")
                var base = random.nextLong().absoluteValue

                while (bases.contains(base))
                    base = random.nextLong().absoluteValue

                database.insert("profiles",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("name", name)
                        put("type", type)
                        put("uri", uri)
                        put("file", file)
                        put("active", active)
                        put("last_update", lastUpdate)
                        put("base", base.toString())
                    })
            }
            cursor.close()

            database.execSQL("DROP TABLE _profiles")

            database.endTransaction()
        }
    }
}