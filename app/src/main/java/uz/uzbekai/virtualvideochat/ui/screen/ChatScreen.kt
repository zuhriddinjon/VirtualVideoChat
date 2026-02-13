@file:kotlin.OptIn(ExperimentalPermissionsApi::class)

package uz.uzbekai.virtualvideochat.ui.screen


import android.Manifest
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.collectLatest
import uz.uzbekai.virtualvideochat.domain.ChatState
import uz.uzbekai.virtualvideochat.domain.VideoType
import uz.uzbekai.virtualvideochat.domain.getVideoType
import uz.uzbekai.virtualvideochat.domain.isMicrophoneActive
import uz.uzbekai.virtualvideochat.domain.shouldLoop
import uz.uzbekai.virtualvideochat.viewmodel.ChatEvent
import uz.uzbekai.virtualvideochat.viewmodel.ChatViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        Log.d("TAGTAG", "ChatScreen: ${state}")
    }

    val micPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChatEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }

                is ChatEvent.RequestMicrophonePermission -> {
                    if (!micPermissionState.status.isGranted) {
                        micPermissionState.launchPermissionRequest()
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseConversation()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeConversation()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                PremiumSnackbar(data)
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            EnhancedVideoPlayer(
                videoType = state.getVideoType(),
                shouldLoop = state.shouldLoop(),
                onVideoCompleted = { videoType ->
                    viewModel.onVideoCompleted(videoType)
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            StatusBar(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            AnimatedVisibility(
                visible = state.isMicrophoneActive(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EnhancedMicrophonePulse()
            }

            PremiumChatControls(
                state = state,
                micPermissionGranted = micPermissionState.status.isGranted,
                onStartChat = {
                    if (micPermissionState.status.isGranted) {
                        viewModel.startChat()
                    } else {
                        viewModel.requestMicrophonePermission()
                    }
                },
                onEndChat = { viewModel.endChat() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun EnhancedVideoPlayer(
    videoType: VideoType,
    shouldLoop: Boolean,
    onVideoCompleted: (VideoType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTransitioning by remember { mutableStateOf(false) }
    val currentShouldLoop by rememberUpdatedState(newValue = shouldLoop)
    val currentVideoType by rememberUpdatedState(newValue = videoType)


    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("TAGTAG", "onPlaybackStateChanged: $playbackState, $shouldLoop")
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                if (!currentShouldLoop) {
                                    onVideoCompleted(currentVideoType)
                                }
                            }

//                            Player.STATE_BUFFERING -> {
//                                isTransitioning = true
//                            }
//
//                            Player.STATE_READY -> {
//                                isTransitioning = false
//                            }
                        }
                    }
                })
            }
    }

    LaunchedEffect(videoType, shouldLoop) {
        val uri = "android.resource://${context.packageName}/${videoType.resourceId}".toUri()
//
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))

        exoPlayer.prepare()
        exoPlayer.play()
//        val index = VideoType.entries.indexOf(videoType)
//        Log.d("TAGTAG", "shouldLoop: $shouldLoop")
//
//        exoPlayer.apply {
//            seekTo(index, 0)
//            repeatMode =
//                if (shouldLoop) Player.REPEAT_MODE_ONE
//                else Player.REPEAT_MODE_OFF
//            playWhenReady = true
//        }
    }

    DisposableEffect(Unit) {
//        val mediaItems = listOf(
//            R.raw.idle,
//            R.raw.greeting,
//            R.raw.listening,
//            R.raw.weather,
//            R.raw.general_response,
//            R.raw.goodbye,
//            R.raw.fallback,
//            R.raw.prompt
//        ).map {
//            val uri = "android.resource://${context.packageName}/${it}".toUri()
//            MediaItem.fromUri(uri)
//        }
//        exoPlayer.setPauseAtEndOfMediaItems(true)
//
//        exoPlayer.setMediaItems(mediaItems, false)
//        exoPlayer.prepare()
        onDispose {
            exoPlayer.release()
        }
    }

    var aspectRatio by remember { mutableStateOf(16f / 9f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height != 0) {
                    aspectRatio =
                        videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }


        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    controllerAutoShow = false
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = {
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(aspectRatio)
        )

        AnimatedVisibility(
            visible = isTransitioning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun StatusBar(
    state: ChatState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn() + slideInVertically() togetherWith
                    fadeOut() + slideOutVertically()
        },
        modifier = modifier
    ) { currentState ->
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.15f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusIcon(state = currentState)
                Text(
                    text = getStatusText(currentState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(state: ChatState) {
    val icon = remember(state) {
        when (state) {
            is ChatState.Idle -> Icons.Rounded.Person
            is ChatState.Greeting -> Icons.Rounded.WavingHand
            is ChatState.Listening -> Icons.Rounded.Hearing
            is ChatState.Response -> Icons.Rounded.Chat
            is ChatState.Goodbye -> Icons.Rounded.ExitToApp
            is ChatState.Error -> Icons.Rounded.Error
        }
    }

    val color = remember(state) {
        when (state) {
            is ChatState.Listening -> Color(0xFF4CAF50)
            is ChatState.Error -> Color(0xFFEF5350)
            else -> Color.White
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (state is ChatState.Listening) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(24.dp)
            .scale(if (state is ChatState.Listening) scale else 1f)
    )
}

private fun getStatusText(state: ChatState): String {
    return when (state) {
        is ChatState.Idle -> "Ready"
        is ChatState.Greeting -> "Greeting..."
        is ChatState.Listening -> "Listening..."
        is ChatState.Response -> "Preparing response..."
        is ChatState.Goodbye -> "Goodbye..."
        is ChatState.Error -> "An error occurred"
    }
}

@Composable
private fun EnhancedMicrophonePulse(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        repeat(3) { index ->
            val delay = index * 500
            val animatedRadius by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radius_$index"
            )

            val animatedAlpha = (1f - animatedRadius).coerceIn(0f, 1f)

            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val radius = size.minDimension / 2 * animatedRadius
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = animatedAlpha * 0.6f),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
        }


        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            tonalElevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.3f),
                                Color(0xFF3F51B5).copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumChatControls(
    state: ChatState,
    micPermissionGranted: Boolean,
    onStartChat: () -> Unit,
    onEndChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )

                        Text(
                            text = getControlStatusText(state, micPermissionGranted),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        AnimatedContent(
                            targetState = state,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                            }
                        ) { currentState ->
                            when (currentState) {
                                is ChatState.Idle -> {
                                    PremiumButton(
                                        text = if (micPermissionGranted) "Start" else "Grant Permission",
                                        icon = if (micPermissionGranted) Icons.Rounded.PlayArrow else Icons.Rounded.Mic,
                                        onClick = onStartChat,
                                        isPrimary = true
                                    )
                                }

                                else -> {
                                    if (currentState !is ChatState.Goodbye) {
                                        PremiumButton(
                                            text = "End",
                                            icon = Icons.Rounded.Stop,
                                            onClick = onEndChat,
                                            isPrimary = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (isPrimary) {
            scale.animateTo(
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale.value),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(32.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isPrimary) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6200EE),
                                Color(0xFF9D46FF)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun getControlStatusText(state: ChatState, micPermissionGranted: Boolean): String {
    return when (state) {
        is ChatState.Idle -> {
            if (micPermissionGranted) {
                "Ready to start the conversation"
            } else {
                "Microphone permission needed"
            }
        }

        is ChatState.Listening -> "I'm listening..."
        is ChatState.Response -> "Getting ready to answer..."
        else -> ""
    }
}

@Composable
private fun PremiumSnackbar(data: SnackbarData) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White.copy(alpha = 0.95f),
        contentColor = Color.Black
    ) {
        Text(
            text = data.visuals.message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}