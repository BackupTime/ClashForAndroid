package com.github.kr328.clash.service.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClashProfileProxyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSelectedForProfile(item: ClashProfileProxyEntity)

    @Query("SELECT * FROM profile_select_proxies WHERE profile_id = :id")
    suspend fun querySelectedForProfile(id: Long): List<ClashProfileProxyEntity>

    @Query("DELETE FROM profile_select_proxies WHERE profile_id = :id AND proxy in (:selected)")
    suspend fun removeSelectedForProfile(id: Int, selected: List<String>)
}