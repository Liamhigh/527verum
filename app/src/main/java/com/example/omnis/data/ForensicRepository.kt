package com.example.omnis.data

import com.example.omnis.model.ForensicCase
import com.example.omnis.model.RunMetadata
import com.example.omnis.model.ReportRenderInput
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ForensicRepository(private val forensicDao: ForensicDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val allCasesFlow: Flow<List<ForensicCase>> = forensicDao.getAllCasesFlow()

    suspend fun getCaseById(id: String): ForensicCase? = withContext(Dispatchers.IO) {
        forensicDao.getCaseById(id)
    }

    suspend fun saveCase(forensicCase: ForensicCase) = withContext(Dispatchers.IO) {
        forensicDao.insertCase(forensicCase)
    }

    suspend fun deleteCaseById(id: String) = withContext(Dispatchers.IO) {
        forensicDao.deleteCaseById(id)
    }

    suspend fun clearAllCases() = withContext(Dispatchers.IO) {
        forensicDao.clearAllCases()
    }

    // Helper serialization methods
    fun serializeRunMetadata(metadata: RunMetadata): String {
        return try {
            val adapter = moshi.adapter(RunMetadata::class.java)
            adapter.toJson(metadata)
        } catch (e: Exception) {
            ""
        }
    }

    fun deserializeRunMetadata(json: String): RunMetadata? {
        if (json.isEmpty()) return null
        return try {
            val adapter = moshi.adapter(RunMetadata::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun serializeReportInput(report: ReportRenderInput): String {
        return try {
            val adapter = moshi.adapter(ReportRenderInput::class.java)
            adapter.toJson(report)
        } catch (e: Exception) {
            ""
        }
    }

    fun deserializeReportInput(json: String): ReportRenderInput? {
        if (json.isEmpty()) return null
        return try {
            val adapter = moshi.adapter(ReportRenderInput::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
