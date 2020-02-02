package com.github.kr328.clash.service.data

import androidx.room.*

@Dao
interface ClashProfileDao {
    @Query("UPDATE profiles SET active = CASE WHEN id = :id THEN 1 ELSE 0 END")
    fun setActiveProfile(id: Long)

    @Query("SELECT * FROM profiles WHERE active = 1 LIMIT 1")
    fun queryActiveProfile(): ClashProfileEntity?

    @Query("SELECT * FROM profiles")
    fun queryProfiles(): Array<ClashProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun queryProfileById(id: Long): ClashProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun addProfile(profile: ClashProfileEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    fun updateProfile(profile: ClashProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    fun removeProfile(id: Long)

    @Query("SELECT id FROM profiles WHERE rowId = :rowId")
    fun getId(rowId: Long): Long
}