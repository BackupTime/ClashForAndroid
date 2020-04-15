package com.github.kr328.clash.service.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.service.data.DatabaseMigrations.VERSION_1_2
import com.github.kr328.clash.service.data.DatabaseMigrations.VERSION_2_3
import com.github.kr328.clash.service.data.DatabaseMigrations.VERSION_3_4
import androidx.room.Database as DatabaseMetadata

@DatabaseMetadata(
    version = 4,
    exportSchema = false,
    entities = [ProfileEntity::class, SelectedProxyEntity::class]
)
abstract class Database : RoomDatabase() {
    abstract fun openProfileDao(): ProfileDao
    abstract fun openSelectedProxyDao(): SelectedProxyDao

    companion object {
        val database = open(Global.application)

        private fun open(context: Context): Database {
            return Room.databaseBuilder(
                context.applicationContext,
                Database::class.java,
                "clash-config"
            ).addMigrations(VERSION_1_2, VERSION_2_3, VERSION_3_4).build()
        }
    }
}