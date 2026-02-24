package com.alignation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alignation.data.model.AlignerSet
import kotlinx.coroutines.flow.Flow

@Dao
interface AlignerSetDao {

    @Insert
    suspend fun insert(set: AlignerSet): Long

    @Query("SELECT * FROM aligner_sets ORDER BY startedAt DESC LIMIT 1")
    fun getCurrentSet(): Flow<AlignerSet?>

    @Query("SELECT * FROM aligner_sets ORDER BY startedAt DESC LIMIT 1")
    suspend fun getCurrentSetOnce(): AlignerSet?

    @Query("SELECT * FROM aligner_sets ORDER BY startedAt DESC")
    fun getAllSets(): Flow<List<AlignerSet>>

    @Query("SELECT * FROM aligner_sets ORDER BY startedAt DESC")
    suspend fun getAllSetsOnce(): List<AlignerSet>
}
