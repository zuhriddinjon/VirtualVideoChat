package uz.uzbekai.virtualvideochat.domain

import androidx.annotation.RawRes
import uz.uzbekai.virtualvideochat.R

enum class VideoType(@RawRes val resourceId: Int) {
    IDLE(R.raw.idle),
    GREETING(R.raw.greeting),
    LISTENING(R.raw.listening),
    WEATHER(R.raw.weather),
    GENERAL_RESPONSE(R.raw.general_response),
    GOODBYE(R.raw.goodbye),
    FALLBACK(R.raw.fallback),
    PROMPT(R.raw.prompt);

    val displayName: String
        get() = name.lowercase().replace('_', ' ')
}