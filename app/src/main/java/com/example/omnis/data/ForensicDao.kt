package com.example.omnis.data

import androidx.room.*
import com.example.omnis.model.ForensicCase
import kotlinx.coroutines.flow.Flow

@Dao
interface ForensicDao {
    @Query("SELECT * FROM forensic_cases ORDER BY dateCreated DESC")
    fun getAllCasesFlow(): Flow<List<ForensicCase>>

    @Query("SELECT * FROM forensic_cases WHERE id = :id")
    suspend fun getCaseById(id: String): ForensicCase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(forensicCase: ForensicCase)

    @Query("DELETE FROM forensic_cases WHERE id = :id")
    suspend fun deleteCaseById(id: String)

    @Query("DELETE FROM forensic_cases")
    suspend fun clearAllCases()
}
