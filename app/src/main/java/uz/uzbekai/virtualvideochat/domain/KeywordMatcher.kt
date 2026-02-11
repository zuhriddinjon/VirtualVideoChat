package uz.uzbekai.virtualvideochat.domain

object KeywordMatcher {

    private val greetingKeywords = setOf(
        "hello", "hi", "hey", "greetings", "good morning",
        "good afternoon", "good evening", "howdy"
    )

    private val weatherKeywords = setOf(
        "weather", "forecast", "temperature", "rain", "sunny",
        "cloudy", "snow", "hot", "cold", "climate"
    )

    private val goodbyeKeywords = setOf(
        "goodbye", "bye", "see you", "later", "end chat",
        "end conversation", "stop", "quit", "exit"
    )

    fun matchKeywords(transcript: String): VideoType {
        val normalized = transcript.lowercase().trim()

        return when {
            goodbyeKeywords.any { keyword -> normalized.contains(keyword) } ->
                VideoType.GOODBYE

            weatherKeywords.any { keyword -> normalized.contains(keyword) } ->
                VideoType.WEATHER

            greetingKeywords.any { keyword -> normalized.contains(keyword) } ->
                VideoType.GENERAL_RESPONSE

            else -> VideoType.GENERAL_RESPONSE
        }
    }

    fun isGoodbye(transcript: String): Boolean {
        val normalized = transcript.lowercase().trim()
        return goodbyeKeywords.any { keyword -> normalized.contains(keyword) }
    }

    fun detectIntent(transcript: String): String {
        val normalized = transcript.lowercase().trim()

        return when {
            goodbyeKeywords.any { normalized.contains(it) } -> "goodbye"
            weatherKeywords.any { normalized.contains(it) } -> "weather_inquiry"
            greetingKeywords.any { normalized.contains(it) } -> "greeting"
            else -> "general_conversation"
        }
    }
}