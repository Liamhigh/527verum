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
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Advisory Mode (Offline Fallback): Gemini API Key not set in Secrets. Running local R&D rules parsing instead. Summary: Rules checked, no overrides permitted under offline state."
        }

        val fullPrompt = "User Question: $prompt\n\nActive Sealed Case Context:\n$reportText"

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = fullPrompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = B9_SYSTEM_PROMPT))
            ),
            generationConfig = GeminiConfig(temperature = 0.3f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Advisory B9 Error: No analysis generated."
        } catch (e: Exception) {
            "API Connection Log: Failed to contact remote brain server (Offline model enforcement active). Details: ${e.localizedMessage}. Local fallback operational."
        }
    }
}
