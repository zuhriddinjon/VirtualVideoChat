package uz.uzbekai.virtualvideochat.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


sealed class SpeechResult {
    object Idle : SpeechResult()
    data class Success(val transcript: String) : SpeechResult()
    data class PartialResult(val transcript: String) : SpeechResult()
    object NoMatch : SpeechResult()
    object NetworkError : SpeechResult()
    object GeneralError : SpeechResult()
}

class SpeechRepository(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _results = MutableStateFlow<SpeechResult>(SpeechResult.Idle)
    val results: StateFlow<SpeechResult> = _results.asStateFlow()

    private companion object {
        const val TAG = "SpeechRepository"
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening, ignoring start request")
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            Log.e(TAG, "Speech recognition not available on this device")
            _results.value = SpeechResult.GeneralError
            return
        }

        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000L
            )
        }

        speechRecognizer?.startListening(intent)
        isListening = true

        Log.d(TAG, "Started listening")
    }

    fun stopListening() {
        if (!isListening) {
            return
        }

        speechRecognizer?.stopListening()
        isListening = false
        _results.value = SpeechResult.Idle

        Log.d(TAG, "Stopped listening")
    }

    fun cancelListening() {
        if (!isListening) {
            return
        }

        speechRecognizer?.cancel()
        isListening = false
        _results.value = SpeechResult.Idle

        Log.d(TAG, "Cancelled listening")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        Log.d(TAG, "Speech recognizer destroyed")
    }

    private val recognitionListener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Could use for visual feedback (amplitude)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Not used
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            isListening = false
        }

        override fun onError(error: Int) {
            isListening = false

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error: $error"
            }

            Log.w(TAG, "Speech error: $errorMessage")

            _results.value = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechResult.NoMatch

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechResult.NetworkError

                else -> SpeechResult.GeneralError
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            if (matches.isNullOrEmpty()) {
                Log.w(TAG, "No results")
                _results.value = SpeechResult.NoMatch
                return
            }

            val bestMatch = matches[0]
            Log.d(TAG, "Final result: $bestMatch")
            _results.value = SpeechResult.Success(bestMatch)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            if (!matches.isNullOrEmpty()) {
                val partial = matches[0]
                Log.d(TAG, "Partial result: $partial")
                _results.value = SpeechResult.PartialResult(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Not used
        }
    }
}