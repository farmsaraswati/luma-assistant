package com.example.lumaassistant.ai

enum class AiProvider(val displayName: String, val keyHint: String, val getKeyUrl: String) {
    GEMINI(
        displayName = "Google Gemini (free)",
        keyHint = "Starts with AIza...",
        getKeyUrl = "aistudio.google.com/apikey"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude (paid)",
        keyHint = "Starts with sk-ant-...",
        getKeyUrl = "console.anthropic.com"
    ),
    OPENROUTER(
        displayName = "OpenRouter (free models available)",
        keyHint = "Starts with sk-or-...",
        getKeyUrl = "openrouter.ai/keys"
    ),
    GROQ(
        displayName = "Groq (free, fast)",
        keyHint = "Starts with gsk_...",
        getKeyUrl = "console.groq.com/keys"
    )
}
