package com.dermify.dermify.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SkinScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: SkinScan): Long

    @Query("SELECT * FROM skin_scans ORDER BY timestamp DESC")
    fun getAllScans(): LiveData<List<SkinScan>>

    @Delete
    suspend fun delete(scan: SkinScan)

    @Query("DELETE FROM skin_scans")
    suspend fun deleteAll()
}