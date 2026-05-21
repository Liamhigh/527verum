package com.example.omnis.data

import com.example.omnis.model.ForensicCase
import com.example.omnis.model.RunMetadata
import com.example.omnis.model.ReportRenderInput
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ForensicRepository(private val forensicDao: ForensicDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun encrypt(caseId: String, data: String): String {
        if (data.isEmpty()) return ""
        return try {
            val keyBytes = (caseId + "VERUM_OMNIS_STRENGTH_2026").toByteArray(Charsets.UTF_8).copyOf(16)
            val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            data
        }
    }

    private fun decrypt(caseId: String, encryptedData: String): String {
        if (encryptedData.isEmpty()) return ""
        return try {
            val keyBytes = (caseId + "VERUM_OMNIS_STRENGTH_2026").toByteArray(Charsets.UTF_8).copyOf(16)
            val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey)
            val decoded = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedData
        }
    }

    private fun decryptCase(c: ForensicCase): ForensicCase {
        return c.copy(
            evidenceContent = decrypt(c.id, c.evidenceContent),
            reportRenderInputJson = decrypt(c.id, c.reportRenderInputJson),
            runMetadataJson = decrypt(c.id, c.runMetadataJson)
        )
    }

    private fun encryptCase(c: ForensicCase): ForensicCase {
        return c.copy(
            evidenceContent = encrypt(c.id, c.evidenceContent),
            reportRenderInputJson = encrypt(c.id, c.reportRenderInputJson),
            runMetadataJson = encrypt(c.id, c.runMetadataJson)
        )
    }

    val allCasesFlow: Flow<List<ForensicCase>> = forensicDao.getAllCasesFlow()
        .map { list -> list.map { decryptCase(it) } }

    suspend fun getCaseById(id: String): ForensicCase? = withContext(Dispatchers.IO) {
        forensicDao.getCaseById(id)?.let { decryptCase(it) }
    }

    suspend fun saveCase(forensicCase: ForensicCase) = withContext(Dispatchers.IO) {
        forensicDao.insertCase(encryptCase(forensicCase))
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
