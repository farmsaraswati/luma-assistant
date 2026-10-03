package com.example.lumaassistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiClient(private val provider: AiProvider, private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(history: List<Pair<String, String>>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    AiProvider.GEMINI -> sendGemini(history)
                    AiProvider.ANTHROPIC -> sendAnthropic(history)
                    AiProvider.OPENROUTER -> sendOpenAiCompatible(
                        history,
                        url = "https://openrouter.ai/api/v1/chat/completions",
                        model = "meta-llama/llama-3.1-8b-instruct:free"
                    )
                    AiProvider.GROQ -> sendOpenAiCompatible(
                        history,
                        url = "https://api.groq.com/openai/v1/chat/completions",
                        model = "llama-3.1-8b-instant"
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun sendGemini(history: List<Pair<String, String>>): Result<String> {
        val contents = JSONArray()
        history.forEach { (role, text) ->
            val geminiRole = if (role == "assistant") "model" else "user"
            contents.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            })
        }
        val body = JSONObject().put("contents", contents)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) return Result.failure(Exception("Gemini error ${response.code}: $responseBody"))
            val text = JSONObject(responseBody)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")
            return Result.success(text)
        }
    }

    private fun sendAnthropic(history: List<Pair<String, String>>): Result<String> {
        val messages = JSONArray()
        history.forEach { (role, text) ->
            messages.put(JSONObject().apply { put("role", role); put("content", text) })
        }
        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1000)
            put("messages", messages)
        }
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) return Result.failure(Exception("Anthropic error ${response.code}: $responseBody"))
            val text = JSONObject(responseBody).getJSONArray("content").getJSONObject(0).getString("text")
            return Result.success(text)
        }
    }

    private fun sendOpenAiCompatible(history: List<Pair<String, String>>, url: String, model: String): Result<String> {
        val messages = JSONArray()
        history.forEach { (role, text) ->
            messages.put(JSONObject().apply { put("role", role); put("content", text) })
        }
        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) return Result.failure(Exception("API error ${response.code}: $responseBody"))
            val text = JSONObject(responseBody)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            return Result.success(text)
        }
    }
}
