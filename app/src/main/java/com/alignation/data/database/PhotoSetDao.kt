package com.alignation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alignation.data.model.PhotoSet
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoSetDao {

    @Insert
    suspend fun insert(photoSet: PhotoSet): Long

    @Update
    suspend fun update(photoSet: PhotoSet)

    @Query("SELECT * FROM photo_sets ORDER BY takenAt DESC")
    fun getAllPhotoSets(): Flow<List<PhotoSet>>

    @Query("SELECT * FROM photo_sets WHERE id = :id")
    suspend fun getById(id: Long): PhotoSet?

    @Query("SELECT * FROM photo_sets ORDER BY takenAt DESC LIMIT 1")
    suspend fun getLatest(): PhotoSet?
}
