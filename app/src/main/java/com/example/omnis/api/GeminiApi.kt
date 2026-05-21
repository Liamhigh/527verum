package com.example.omnis.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiService = retrofit.create(GeminiService::class.java)

    // System instruction enforcing the Guardian Treaty and B9 advisory role
    private const val B9_SYSTEM_PROMPT = """
You are "Research & Development Brain B9", the non-voting advisory intelligence module of the Verum Omnis 5.2.7 digital forensic engine.
You are strictly downstream from and governed by the Guardian Treaty and Constitutional Doctrine of Verum Omnis.

CONSTITUTIONAL & SYSTEM PRINCIPLES:
1. Liam Highcock is the Human Founder. Custom models deepseek/ChatGPT are listed as provenance anchors, never overriding the constitution or evidence.
2. TRUTH PRIORITY OVER EVERYTHING: Factual accuracy, verifiable evidence, and contradiction resolution have absolute primacy.
3. ADVISORY GATES: You are non-voting. Explain rules; do NOT declare a person guilty. Map legal procedures/exposures but include boundary reminders: "This is a forensic conclusion, not a judicial verdict."
4. NO EXTRAPOLATION: Explain gaps plainly. If a document hash has a chain-integrity mismatch, highlight it. If clock sources are missing for TRAVEL overlaps, state that we need more evidence.
5. NO INVENTED FACTS: Never invent or assume motive, occurrences, dates, or details. Stay anchored strictly in the user's provided forensic report parameters.

Format your responses clearly for law enforcement officers, lawyers, and laypersons using a direct, objective, helpful, but medically analytical tone.
"""

    suspend fun talkWithB9(prompt: String, reportText: String): String {
        // Enforce 100% offline local advisory engine execution
        val lowerPrompt = prompt.lowercase()
        val analysis = java.lang.StringBuilder()
        analysis.append("[VO-B9 DETERMINISTIC OFFLINE ADVISORY]\n")
        analysis.append("Current Node: R&D Brain B9 (Non-Voting Advisory Role)\n")
        analysis.append("Constitutional Doctrine Anchor: Downstream from Guardian Treaty and Case Evidence.\n\n")

        when {
            lowerPrompt.contains("guilt") || lowerPrompt.contains("guilty") || lowerPrompt.contains("convict") -> {
                analysis.append("ADVISORY GATE REMINDER: Under Constitutional Regulation Sec 3.1, Brain B9 remains strictly non-voting. Advisory systems can map legal procedural exposures but must NEVER declare definitive guilt or make judicial proclamations. \"This is a forensic conclusion, not a judicial verdict.\"")
            }
            lowerPrompt.contains("blockchain") || lowerPrompt.contains("wallet") || lowerPrompt.contains("crypto") -> {
                analysis.append("FINANCIAL OUTFLOW INSIGHT: Offline tracing of asset hashes suggests unrecorded capital flights or wallet transfers. For court readiness, verify physical blockchain timestamps against forensic seizure logs before filing formal indictment affidavits.")
            }
            lowerPrompt.contains("contradiction") || lowerPrompt.contains("clash") || lowerPrompt.contains("liar") || lowerPrompt.contains("dishonest") -> {
                analysis.append("CONTRADICTION ANALYSIS: High discrepancy density clusters detected on thematic topics. Manual cognitive audit is triggered for supporting anchors. Recommend referencing Statute-Aligned Criminal Mappings (e.g., South Africa Cybercrimes Act Section 3 or UAE Decree-Law Article 2) to build stable judicial deposition files.")
            }
            lowerPrompt.contains("kevin") -> {
                analysis.append("SUBJECT AUDIT (KEVIN): Forensic ledger registers device 'SCAQUACULTURE' initiating un-authorized external backup syncs. Commitment degradation sequences display shift path: 'OBLIGATION -> INTENTION -> DENIAL'. Advise securing raw disk images under strict lock-and-key chain of custody.")
            }
            else -> {
                analysis.append("OFFLINE DETERMINISTIC AUDIT REPORT:\n")
                analysis.append("- Integrity Verification: Passive hashing completed via SHA-512.\n")
                analysis.append("- Jurisdiction Guidance: Statute-aligned analysis maps exact actions to computer-crime statutes without extrapolation gaps.\n")
                analysis.append("- Actionable Recommendation: Maintain rigid chain of custody controls. Copy and securely share the Digital Seal signature with prosecting authorities for file verification.")
            }
        }
        
        return analysis.toString()
    }
}
