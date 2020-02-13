package com.github.kr328.clash.service.data

import androidx.room.*

@Dao
interface ClashProfileDao {
    @Query("UPDATE profiles SET active = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(id: Long)

    @Query("SELECT * FROM profiles WHERE active = 1 LIMIT 1")
    suspend fun queryActiveProfile(): ClashProfileEntity?

    @Query("SELECT * FROM profiles")
    suspend fun queryProfiles(): Array<ClashProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun queryProfileById(id: Long): ClashProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addProfile(profile: ClashProfileEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateProfile(profile: ClashProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun removeProfile(id: Long)

    @Query("SELECT id FROM profiles WHERE rowId = :rowId")
    suspend fun getId(rowId: Long): Long

    @Query("SELECT IfNull(MAX(id) + 1, 0) AS id FROM profiles")
    suspend fun generateNewId(): Long
}