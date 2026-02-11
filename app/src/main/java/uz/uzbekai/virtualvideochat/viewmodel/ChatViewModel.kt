package uz.uzbekai.virtualvideochat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.uzbekai.virtualvideochat.domain.ChatState
import uz.uzbekai.virtualvideochat.domain.KeywordMatcher
import uz.uzbekai.virtualvideochat.domain.VideoType
import uz.uzbekai.virtualvideochat.repository.SpeechRepository
import uz.uzbekai.virtualvideochat.repository.SpeechResult


class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val speechRepository = SpeechRepository(application)

    private val _state = MutableStateFlow<ChatState>(ChatState.Idle)
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()


    private var silenceJob: Job? = null
    private var silencePromptCount = 0

    companion object {
        const val SILENCE_TIMEOUT_MS = 8000L
        const val MAX_SILENCE_PROMPTS = 2
        private const val TAG = "ChatViewModel"
    }

    init {
        observeSpeechResults()
    }


    private fun observeSpeechResults() {
        viewModelScope.launch {
            speechRepository.results.collect { result ->
                when (result) {
                    is SpeechResult.Success -> handleSpeechSuccess(result.transcript)
                    is SpeechResult.PartialResult -> handlePartialResult(result.transcript)
                    is SpeechResult.NoMatch -> handleNoMatch()
                    is SpeechResult.NetworkError -> handleNetworkError()
                    is SpeechResult.GeneralError -> handleGeneralError()
                    is SpeechResult.Idle -> { /* Hech narsa */
                    }
                }
            }
        }
    }


    fun startChat() {
        if (_state.value != ChatState.Idle) {
            android.util.Log.w(TAG, "Allaqachon suhbat boshlangan")
            return
        }

        _state.value = ChatState.Greeting
        android.util.Log.d(TAG, "Suhbat boshlandi - Greeting holatiga o'tish")
    }

    fun onVideoCompleted(completedVideo: VideoType) {
        android.util.Log.d(TAG, "Video tugadi: ${completedVideo.displayName}")

        when (_state.value) {
            ChatState.Greeting -> {
                transitionToListening()
            }

            is ChatState.Response -> {
                val responseState = _state.value as ChatState.Response

                if (responseState.videoType == VideoType.GOODBYE) {
                    transitionToIdle()
                } else if (responseState.videoType == VideoType.PROMPT) {
                    silencePromptCount++
                    if (silencePromptCount >= MAX_SILENCE_PROMPTS) {
                        _state.value = ChatState.Response(VideoType.GOODBYE)
                    } else {
                        transitionToListening()
                    }
                } else {
                    transitionToListening()
                }
            }

            ChatState.Goodbye -> {
                transitionToIdle()
            }

            else -> {
                android.util.Log.w(TAG, "Kutilmagan video tugashi: ${_state.value}")
            }
        }
    }


    private fun handleSpeechSuccess(transcript: String) {
        cancelSilenceDetection()

        android.util.Log.d(TAG, "Nutq tanildi: $transcript")

        val responseVideo = KeywordMatcher.matchKeywords(transcript)
        val intent = KeywordMatcher.detectIntent(transcript)

        android.util.Log.d(TAG, "Intent: $intent -> ${responseVideo.displayName}")

        _state.value = ChatState.Response(responseVideo)

        speechRepository.stopListening()
    }


    private fun handlePartialResult(partial: String) {
        if (_state.value is ChatState.Listening) {
            cancelSilenceDetection()
            startSilenceDetection()
            android.util.Log.d(TAG, "Qisman natija, timer qayta o'rnatildi: $partial")
        }
    }


    private fun handleNoMatch() {
        android.util.Log.w(TAG, "Nutq tanilmadi")
        _state.value = ChatState.Response(VideoType.FALLBACK)
        speechRepository.stopListening()
        cancelSilenceDetection()
    }


    private fun handleNetworkError() {
        android.util.Log.e(TAG, "Tarmoq xatosi")
        viewModelScope.launch {
            _events.emit(ChatEvent.ShowError("Tarmoq xatosi. Internetni tekshiring."))
        }
        _state.value = ChatState.Response(VideoType.FALLBACK)
        speechRepository.stopListening()
        cancelSilenceDetection()
    }


    private fun handleGeneralError() {
        android.util.Log.e(TAG, "Umumiy xato")
        _state.value = ChatState.Response(VideoType.FALLBACK)
        speechRepository.stopListening()
        cancelSilenceDetection()
    }


    private fun transitionToListening() {
        val currentPromptCount = if (_state.value is ChatState.Listening) {
            (_state.value as ChatState.Listening).promptCount
        } else {
            0
        }

        _state.value = ChatState.Listening(currentPromptCount)

        viewModelScope.launch {
            delay(300)
            startListening()
        }
    }


    fun startListening() {
        if (_state.value !is ChatState.Listening) {
            android.util.Log.w(TAG, "Listening holatida emas")
            return
        }

        speechRepository.startListening()
        startSilenceDetection()
        android.util.Log.d(TAG, "Tinglash boshlandi")
    }


    private fun startSilenceDetection() {
        cancelSilenceDetection()

        silenceJob = viewModelScope.launch {
            delay(SILENCE_TIMEOUT_MS)
            onSilenceDetected()
        }
    }


    private fun cancelSilenceDetection() {
        silenceJob?.cancel()
        silenceJob = null
    }


    private fun onSilenceDetected() {
        android.util.Log.d(TAG, "Jimlik (${silencePromptCount + 1}/$MAX_SILENCE_PROMPTS)")

        speechRepository.stopListening()
        _state.value = ChatState.Response(VideoType.PROMPT)
    }


    private fun transitionToIdle() {
        _state.value = ChatState.Idle
        silencePromptCount = 0
        android.util.Log.d(TAG, "Idle holatiga qaytildi")
    }


    fun pauseConversation() {
        speechRepository.stopListening()
        cancelSilenceDetection()
        android.util.Log.d(TAG, "Suhbat pauzada")
    }


    fun resumeConversation() {
        if (_state.value is ChatState.Listening) {
            startListening()
            android.util.Log.d(TAG, "Suhbat davom ettirildi")
        }
    }


    fun requestMicrophonePermission() {
        viewModelScope.launch {
            _events.emit(ChatEvent.RequestMicrophonePermission)
        }
    }


    fun endChat() {
        speechRepository.stopListening()
        cancelSilenceDetection()
        _state.value = ChatState.Response(VideoType.GOODBYE)
        android.util.Log.d(TAG, "Suhbat qo'lda tugatildi")
    }

    override fun onCleared() {
        super.onCleared()
        speechRepository.destroy()
        cancelSilenceDetection()
        android.util.Log.d(TAG, "ViewModel tozalandi")
    }
}


sealed class ChatEvent {
    data class ShowError(val message: String) : ChatEvent()
    object RequestMicrophonePermission : ChatEvent()
}
