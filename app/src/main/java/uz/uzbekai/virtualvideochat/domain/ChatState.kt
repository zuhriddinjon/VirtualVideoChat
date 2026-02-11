package uz.uzbekai.virtualvideochat.domain

sealed class ChatState {

    object Idle : ChatState()

    object Greeting : ChatState()

    data class Listening(val promptCount: Int = 0) : ChatState()

    data class Response(val videoType: VideoType) : ChatState()

    object Goodbye : ChatState()

    data class Error(val message: String) : ChatState()
}

fun ChatState.shouldLoop(): Boolean = when (this) {
    is ChatState.Idle, is ChatState.Listening -> true
    else -> false
}

fun ChatState.isMicrophoneActive(): Boolean = when (this) {
    is ChatState.Listening -> true
    else -> false
}

fun ChatState.getVideoType(): VideoType = when (this) {
    ChatState.Idle -> VideoType.IDLE
    ChatState.Greeting -> VideoType.GREETING
    is ChatState.Listening -> VideoType.LISTENING
    is ChatState.Response -> this.videoType
    ChatState.Goodbye -> VideoType.GOODBYE
    is ChatState.Error -> VideoType.FALLBACK
}