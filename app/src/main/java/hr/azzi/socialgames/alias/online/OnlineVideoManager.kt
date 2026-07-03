package hr.azzi.socialgames.alias.online

import android.content.Context
import android.util.Log
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Thin wrapper over the Stream Video Android SDK for a room's audio/video call —
 * Android counterpart of iOS `OnlineVideo.swift`. The Stream userId is the Firebase
 * uid (== OnlinePlayer.id), so tiles are matched by player id.
 *
 * `micOn`/`cameraOn` are bound to Stream's real `isEnabled` state, so a denied
 * permission (or a failed enable) is reflected truthfully — no optimistic drift.
 */
class OnlineVideoManager(private val appContext: Context) {

    companion object {
        /** Stream project API key (public — same value the token server uses). */
        const val API_KEY = "vw26vbyf2mh8"
        private const val TAG = "OnlineVideo"
    }

    /** Fetches a Stream token for the given user id (== Firebase uid). Set by OnlineStore. */
    var tokenProvider: (suspend (userId: String) -> String?)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var client: StreamVideo? = null
    var call: Call? = null
        private set

    private val _micOn = MutableStateFlow(true)
    val micOn: StateFlow<Boolean> = _micOn.asStateFlow()
    private val _cameraOn = MutableStateFlow(true)
    val cameraOn: StateFlow<Boolean> = _cameraOn.asStateFlow()

    /** Live participants keyed by Stream userId (== OnlinePlayer.id), for rendering. */
    private val _participants = MutableStateFlow<Map<String, ParticipantState>>(emptyMap())
    val participants: StateFlow<Map<String, ParticipantState>> = _participants.asStateFlow()

    /** User ids currently speaking (for the mic indicator). */
    private val _speakingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val speakingUserIds: StateFlow<Set<String>> = _speakingUserIds.asStateFlow()

    /** User ids whose microphone is OFF (muted) — so remote mutes are visible. */
    private val _mutedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedUserIds: StateFlow<Set<String>> = _mutedUserIds.asStateFlow()

    fun start(userId: String, name: String, callId: String) {
        if (call != null) return   // already in a call
        scope.launch {
            val token = tokenProvider?.invoke(userId)
            if (token.isNullOrEmpty()) {
                Log.w(TAG, "no Stream token for userId $userId (token server unavailable).")
                return@launch
            }
            // Only one StreamVideo instance may exist per process — clear any leftover.
            runCatching { StreamVideo.instanceOrNull()?.cleanup() }
            val streamClient = try {
                StreamVideoBuilder(
                    context = appContext,
                    apiKey = API_KEY,
                    user = User(id = userId, name = name),
                    token = token,
                ).build()
            } catch (e: Exception) {
                Log.w(TAG, "Stream client build failed: ${e.message}"); return@launch
            }
            client = streamClient
            val newCall = streamClient.call("default", callId)
            call = newCall
            try {
                newCall.join(create = true)
                newCall.camera.setEnabled(true)
                newCall.microphone.setEnabled(true)
                newCall.speaker.setSpeakerPhone(true)   // route audio to the loud speaker
                observe(newCall)
            } catch (e: Exception) {
                Log.w(TAG, "Stream join failed: ${e.message}")
            }
        }
    }

    private data class ParticipantFlags(val userId: String, val audioOn: Boolean, val speaking: Boolean)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe(call: Call) {
        val state = call.state
        scope.launch {
            state.participants.collect { list -> _participants.value = list.associateBy { it.userId.value } }
        }
        scope.launch {
            state.participants.flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList())
                else combine(
                    list.map { p ->
                        combine(p.userId, p.audioEnabled, p.speaking) { uid, audio, speaking ->
                            ParticipantFlags(uid, audio, speaking)
                        }
                    },
                ) { it.toList() }
            }.collect { flags ->
                _mutedUserIds.value = flags.filter { !it.audioOn }.map { it.userId }.toSet()
                _speakingUserIds.value = flags.filter { it.speaking }.map { it.userId }.toSet()
            }
        }
        // Bind our own mic/camera toggles to the SDK's real track state.
        scope.launch { call.microphone.isEnabled.collect { _micOn.value = it } }
        scope.launch { call.camera.isEnabled.collect { _cameraOn.value = it } }
    }

    fun setMic(on: Boolean) { runCatching { call?.microphone?.setEnabled(on) } }
    fun setCamera(on: Boolean) { runCatching { call?.camera?.setEnabled(on) } }
    fun switchCamera() { runCatching { call?.camera?.flip() } }

    fun leave() {
        runCatching { client?.cleanup() }   // disconnects the call + tears down the singleton
        scope.coroutineContext.cancelChildren()
        call = null
        client = null
        _participants.value = emptyMap()
        _speakingUserIds.value = emptySet()
        _mutedUserIds.value = emptySet()
        _micOn.value = true
        _cameraOn.value = true
    }
}
