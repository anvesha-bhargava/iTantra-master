package com.iTantra.app

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

import androidx.core.content.ContextCompat

import com.iTantra.app.audio.AudioPlayer
import com.iTantra.app.audio.AudioRecorder
import com.iTantra.app.domain.Language

import com.iTantra.app.ml.stt.SherpaSttEngine
import com.iTantra.app.ml.tts.PiperTtsEngine

import com.iTantra.app.room.RoomMeshManager

import com.iTantra.app.transport.bluetooth.BluetoothPermissionHelper
import com.iTantra.app.transport.connection.CommunicationService
import com.iTantra.app.transport.connection.ConnectionState
import com.iTantra.app.transport.protocol.Message

import com.iTantra.app.ui.about.AboutScreen
import com.iTantra.app.ui.connect.ConnectDeviceScreen
import com.iTantra.app.ui.connect.ConnectingScreen

import com.iTantra.app.ui.history.HistoryScreen
import com.iTantra.app.ui.history.RecordingDirection
import com.iTantra.app.ui.history.TextBackupStore
import com.iTantra.app.ui.history.TextHistoryEntry

import com.iTantra.app.ui.onboarding.PermissionScreen

import com.iTantra.app.ui.radio.MainRadioScreen
import com.iTantra.app.ui.radio.RadioState

import com.iTantra.app.ui.room.CreateRoomScreen
import com.iTantra.app.ui.room.JoinRoomScreen
import com.iTantra.app.ui.room.RoomScreen
import com.iTantra.app.ui.room.RoomSessionScreen

import com.iTantra.app.ui.settings.SettingsScreen
import com.iTantra.app.ui.splash.SplashScreen
import com.iTantra.app.ui.theme.ITantraTheme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.util.UUID


// =============================================================
// APP ROUTES
// =============================================================

private enum class AppScreen {

    SPLASH,

    PERMISSIONS,

    CONNECT,

    CONNECTING,

    RADIO,

    HISTORY,

    SETTINGS,

    ABOUT,

    ROOM,

    CREATE_ROOM,

    JOIN_ROOM,

    ROOM_SESSION
}


// =============================================================
// MAIN ACTIVITY
// =============================================================

class MainActivity : ComponentActivity() {


    // =========================================================
    // AUDIO + LOCAL ML
    // =========================================================

    private val audioRecorder by lazy {

        AudioRecorder(
            applicationContext
        )
    }


    private val audioPlayer by lazy {

        AudioPlayer()
    }


    /*
     * Current repository model:
     *
     * assets/models/dolphin/model.int8.onnx
     *
     * Later you can replace SherpaSttEngine internally with
     * language-specific Whisper engines without changing the
     * transport / history / Room code below.
     */

    private val sttEngine by lazy {

        SherpaSttEngine(
            applicationContext
        )
    }


    /*
     * Current repository contains a Hindi Piper voice.
     */

    private val ttsEngine by lazy {

        PiperTtsEngine(
            applicationContext
        )
    }


    // =========================================================
    // HISTORY
    // =========================================================

    private val textBackupStore by lazy {

        TextBackupStore(
            applicationContext
        )
    }


    // =========================================================
    // COMMUNICATION
    // =========================================================

    private val communicationScope =

        CoroutineScope(

            SupervisorJob() +

                    Dispatchers.Main
        )


    /*
     * This must be the rewritten mesh-capable
     * CommunicationService.
     */

    private val communicationService =

        CommunicationService(

            scope =
                communicationScope
        )


    // =========================================================
    // ROOM / MESH
    // =========================================================

    private val roomMeshManager by lazy {

        RoomMeshManager(

            context =
                applicationContext,

            scope =
                communicationScope,

            communicationService =
                communicationService
        )
    }


    // =========================================================
    // RECORDING JOB
    // =========================================================

    private var recordingJob:
            Job? =
        null


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private val microphonePermissionLauncher =

        registerForActivityResult(

            ActivityResultContracts
                .RequestPermission()

        ) { granted ->

            Log.d(

                "PERMISSION",

                "Microphone permission: $granted"
            )
        }


    private val bluetoothPermissionLauncher =

        registerForActivityResult(

            ActivityResultContracts
                .RequestMultiplePermissions()

        ) { permissions ->

            Log.d(

                "PERMISSION",

                "Bluetooth permissions: $permissions"
            )


            /*
             * Start the local RFCOMM server immediately after
             * Bluetooth permissions are granted.
             */

            if (

                BluetoothPermissionHelper
                    .hasRequiredPermissions(
                        applicationContext
                    )
            ) {

                startBluetoothServer()
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(

        savedInstanceState:
        Bundle?

    ) {

        super.onCreate(
            savedInstanceState
        )


        setContent {

            ITantraTheme {


                // =================================================
                // NAVIGATION
                // =================================================

                var screen by remember {

                    mutableStateOf(
                        AppScreen.SPLASH
                    )
                }


                var returnScreen by remember {

                    mutableStateOf(
                        AppScreen.RADIO
                    )
                }


                var aboutReturnScreen by remember {

                    mutableStateOf(
                        AppScreen.RADIO
                    )
                }


                // =================================================
                // DIRECT DEVICE
                // =================================================

                var selectedDeviceName by remember {

                    mutableStateOf("")
                }


                var selectedDeviceAddress by remember {

                    mutableStateOf("")
                }


                var selectedBluetoothDevice by remember {

                    mutableStateOf<
                            BluetoothDevice?
                            >(
                        null
                    )
                }


                var pairedDeviceNames by remember {

                    mutableStateOf(
                        emptyList<String>()
                    )
                }


                // =================================================
                // LANGUAGES
                // =================================================

                var speakingLanguage by remember {

                    mutableStateOf(
                        Language.HINDI
                    )
                }


                var listeningLanguage by remember {

                    mutableStateOf(
                        Language.HINDI
                    )
                }


                // =================================================
                // RADIO STATE
                // =================================================

                var radioState by remember {

                    mutableStateOf(
                        RadioState.READY
                    )
                }


                // =================================================
                // HISTORY STATE
                // =================================================

                var textHistory by remember {

                    mutableStateOf<
                            List<TextHistoryEntry>
                            >(
                        emptyList()
                    )
                }


                // =================================================
                // ROOM UI STATE
                // =================================================

                var generatedRoomId by remember {

                    mutableStateOf<String?>(
                        null
                    )
                }


                var roomError by remember {

                    mutableStateOf<String?>(
                        null
                    )
                }


                var isJoiningRoom by remember {

                    mutableStateOf(
                        false
                    )
                }


                // =================================================
                // LIVE TRANSPORT STATE
                // =================================================

                val connectedPeers by

                communicationService
                    .connectedPeers
                    .collectAsState()


                val activeRoom by

                roomMeshManager
                    .activeRoom
                    .collectAsState()


                /*
                 * These wrappers let long-running Flow collectors see
                 * the newest Compose values without being restarted.
                 */

                val currentListeningLanguage by

                rememberUpdatedState(
                    listeningLanguage
                )


                val currentActiveRoomId by

                rememberUpdatedState(
                    activeRoom?.roomId
                )


                // =================================================
                // HELPERS
                // =================================================

                fun refreshHistory() {

                    textHistory =

                        textBackupStore
                            .getLastHourEntries()
                }


                fun languageFromName(
                    value: String
                ): Language {

                    return Language
                        .entries
                        .firstOrNull {

                            it.displayName
                                .equals(
                                    value,
                                    ignoreCase = true
                                )
                        }
                        ?: Language.HINDI
                }


                fun displayNameFromCode(
                    value: String
                ): String {

                    return Language
                        .entries
                        .firstOrNull {

                            it.code
                                .equals(
                                    value,
                                    ignoreCase = true
                                )
                        }
                        ?.displayName
                        ?: value
                }


                // =================================================
                // INITIAL HISTORY
                // =================================================

                LaunchedEffect(Unit) {

                    refreshHistory()
                }


                // =================================================
                // SPLASH TIMER
                // =================================================

                LaunchedEffect(Unit) {

                    delay(
                        1400
                    )


                    screen =
                        AppScreen.PERMISSIONS
                }


                // =================================================
                // CONNECTION STATE
                // =================================================

                LaunchedEffect(Unit) {

                    communicationService
                        .connectionState
                        .collect { state ->


                            Log.d(

                                "BLUETOOTH",

                                "Connection state = $state"
                            )


                            when (state) {


                                ConnectionState.ERROR -> {

                                    if (

                                        screen ==
                                        AppScreen.CONNECTING
                                    ) {

                                        radioState =
                                            RadioState.ERROR


                                        screen =
                                            AppScreen.CONNECT
                                    }
                                }


                                ConnectionState.UNAVAILABLE -> {

                                    radioState =
                                        RadioState.ERROR
                                }


                                else -> {

                                    /*
                                     * CONNECTED navigation is handled
                                     * by connectedPeers below so that
                                     * we can confirm the actual selected
                                     * peer joined the peer set.
                                     */
                                }
                            }
                        }
                }


                // =================================================
                // SELECTED DEVICE CONNECTED
                // =================================================

                LaunchedEffect(

                    screen,

                    selectedDeviceAddress,

                    connectedPeers

                ) {

                    if (

                        screen ==
                        AppScreen.CONNECTING &&

                        selectedDeviceAddress
                            .isNotBlank() &&

                        connectedPeers.any {

                            it.address ==
                                    selectedDeviceAddress
                        }
                    ) {

                        radioState =
                            RadioState.READY


                        screen =
                            AppScreen.RADIO
                    }
                }


                // =================================================
                // RECEIVE DIRECT / ROOM TEXT
                // =================================================

                LaunchedEffect(Unit) {

                    communicationService
                        .incomingMessages
                        .collect { message ->


                            if (

                                message.text
                                    .isBlank()
                            ) {

                                return@collect
                            }


                            // -----------------------------------------
                            // ROOM FILTER
                            // -----------------------------------------

                            if (

                                message.type ==
                                Message.TYPE_ROOM_TEXT &&

                                currentActiveRoomId !=
                                message.roomId
                            ) {

                                /*
                                 * The device may still relay this packet
                                 * at the transport layer, but it should not
                                 * play/log a Room it did not join.
                                 */

                                return@collect
                            }


                            val sourceLanguageName =

                                displayNameFromCode(
                                    message.language
                                )


                            // -----------------------------------------
                            // SAVE RECEIVED TEXT
                            // -----------------------------------------

                            textBackupStore.logText(

                                messageId =
                                    message.id,

                                direction =
                                    RecordingDirection.RECEIVED,

                                speakingLanguage =
                                    sourceLanguageName,

                                listeningLanguage =
                                    currentListeningLanguage
                                        .displayName,

                                originalText =
                                    message.text,

                                translatedText =
                                    "",

                                roomId =
                                    message.roomId,

                                timestamp =
                                    message.timestamp
                            )


                            refreshHistory()


                            radioState =
                                RadioState.RECEIVING


                            // -----------------------------------------
                            // CURRENT TTS LIMITATION
                            //
                            // Repository only contains Hindi Piper TTS.
                            // Until translation and additional voices are
                            // added, speak only Hindi -> Hindi locally.
                            // -----------------------------------------

                            if (

                                message.language ==
                                Language.HINDI.code &&

                                currentListeningLanguage ==
                                Language.HINDI
                            ) {

                                communicationScope.launch(

                                    Dispatchers.Default

                                ) {

                                    try {

                                        Handler(
                                            Looper.getMainLooper()
                                        ).post {

                                            radioState =
                                                RadioState.PLAYING
                                        }


                                        val audio =

                                            ttsEngine
                                                .synthesize(
                                                    message.text
                                                )


                                        if (

                                            audio.samples
                                                .isNotEmpty()
                                        ) {

                                            audioPlayer.play(

                                                samples =
                                                    audio.samples,

                                                sampleRate =
                                                    audio.sampleRate
                                            )
                                        }


                                        Handler(
                                            Looper.getMainLooper()
                                        ).post {

                                            radioState =
                                                RadioState.READY
                                        }

                                    } catch (
                                        e: Exception
                                    ) {

                                        Log.e(

                                            "TTS",

                                            "TTS failed",

                                            e
                                        )


                                        Handler(
                                            Looper.getMainLooper()
                                        ).post {

                                            radioState =
                                                RadioState.ERROR
                                        }
                                    }
                                }

                            } else {

                                radioState =
                                    RadioState.READY
                            }
                        }
                }


                // =================================================
                // ROUTER
                // =================================================

                when (screen) {


                    // =================================================
                    // SPLASH
                    // =================================================

                    AppScreen.SPLASH -> {

                        SplashScreen()
                    }


                    // =================================================
                    // PERMISSIONS
                    // =================================================

                    AppScreen.PERMISSIONS -> {

                        PermissionScreen(

                            onContinue = {

                                requestAllPermissions()


                                screen =
                                    AppScreen.CONNECT
                            }
                        )
                    }


                    // =================================================
                    // CONNECT
                    // =================================================

                    AppScreen.CONNECT -> {


                        LaunchedEffect(Unit) {

                            startBluetoothServer()


                            pairedDeviceNames =

                                getPairedDeviceNames()
                        }


                        ConnectDeviceScreen(

                            devices =
                                pairedDeviceNames,


                            onDeviceClick = {
                                    deviceName ->


                                val device =

                                    findPairedDevice(
                                        deviceName
                                    )


                                if (
                                    device == null
                                ) {

                                    radioState =
                                        RadioState.ERROR

                                } else {

                                    selectedBluetoothDevice =
                                        device


                                    selectedDeviceName =
                                        deviceName


                                    selectedDeviceAddress =

                                        try {

                                            device.address

                                        } catch (
                                            _: SecurityException
                                        ) {

                                            ""
                                        }


                                    screen =
                                        AppScreen.CONNECTING
                                }
                            },


                            onScanAgain = {

                                pairedDeviceNames =

                                    getPairedDeviceNames()
                            }
                        )
                    }


                    // =================================================
                    // CONNECTING
                    // =================================================

                    AppScreen.CONNECTING -> {

                        ConnectingScreen(

                            deviceName =
                                selectedDeviceName
                        )


                        LaunchedEffect(

                            selectedDeviceAddress
                        ) {

                            val device =

                                selectedBluetoothDevice


                            if (
                                device == null
                            ) {

                                screen =
                                    AppScreen.CONNECT


                                return@LaunchedEffect
                            }


                            try {

                                /*
                                 * Does NOT disconnect the server or other
                                 * mesh neighbours in the rewritten manager.
                                 */

                                communicationService
                                    .connectToDevice(
                                        device
                                    )

                            } catch (
                                e: Exception
                            ) {

                                Log.e(

                                    "BLUETOOTH",

                                    "Connection failed",

                                    e
                                )


                                radioState =
                                    RadioState.ERROR


                                screen =
                                    AppScreen.CONNECT
                            }
                        }
                    }


                    // =================================================
                    // DIRECT RADIO
                    // =================================================

                    AppScreen.RADIO -> {

                        MainRadioScreen(

                            state =
                                radioState,


                            connectedDevice =
                                selectedDeviceName,


                            connectedDeviceCount =
                                connectedPeers.size,


                            speakingLanguage =
                                speakingLanguage.displayName,


                            listeningLanguage =
                                listeningLanguage.displayName,


                            onSpeakingLanguageChange = {

                                speakingLanguage =

                                    languageFromName(
                                        it
                                    )
                            },


                            onListeningLanguageChange = {

                                listeningLanguage =

                                    languageFromName(
                                        it
                                    )
                            },


                            onRoomClick = {

                                radioState =
                                    RadioState.READY


                                screen =
                                    AppScreen.ROOM
                            },


                            onHistoryClick = {

                                returnScreen =
                                    AppScreen.RADIO


                                refreshHistory()


                                screen =
                                    AppScreen.HISTORY
                            },


                            onSettingsClick = {

                                returnScreen =
                                    AppScreen.RADIO


                                screen =
                                    AppScreen.SETTINGS
                            },


                            onAboutClick = {

                                aboutReturnScreen =
                                    AppScreen.RADIO


                                screen =
                                    AppScreen.ABOUT
                            },


                            onStartTalking = {

                                if (

                                    radioState ==
                                    RadioState.READY
                                ) {

                                    radioState =
                                        RadioState.LISTENING


                                    startRecording(

                                        roomMode =
                                            false,

                                        peerAddress =
                                            selectedDeviceAddress,

                                        roomId =
                                            null,

                                        speakingLanguage =
                                            speakingLanguage,

                                        listeningLanguage =
                                            listeningLanguage,

                                        onStateChange = {

                                            radioState =
                                                it
                                        },

                                        onHistoryChanged = {

                                            refreshHistory()
                                        }
                                    )
                                }
                            },


                            onStopTalking = {

                                stopRecording()
                            }
                        )
                    }


                    // =================================================
                    // HISTORY
                    // =================================================

                    AppScreen.HISTORY -> {

                        HistoryScreen(

                            entries =
                                textHistory,


                            onBackClick = {

                                screen =
                                    returnScreen
                            },


                            onClearHistory = {

                                textBackupStore
                                    .clearHistory()


                                refreshHistory()
                            }
                        )
                    }


                    // =================================================
                    // ROOM HOME
                    // =================================================

                    AppScreen.ROOM -> {

                        RoomScreen(

                            onBackClick = {

                                screen =
                                    AppScreen.RADIO
                            },


                            onCreateRoomClick = {

                                generatedRoomId =
                                    null


                                roomError =
                                    null


                                screen =
                                    AppScreen.CREATE_ROOM
                            },


                            onJoinRoomClick = {

                                roomError =
                                    null


                                screen =
                                    AppScreen.JOIN_ROOM
                            }
                        )
                    }


                    // =================================================
                    // CREATE ROOM
                    // =================================================

                    AppScreen.CREATE_ROOM -> {

                        CreateRoomScreen(

                            onBackClick = {

                                generatedRoomId =
                                    null


                                roomError =
                                    null


                                screen =
                                    AppScreen.ROOM
                            },


                            generatedRoomId =
                                generatedRoomId,


                            errorMessage =
                                roomError,


                            onCreateRoom = {
                                    roomName ->


                                try {

                                    val room =

                                        roomMeshManager
                                            .createRoom(
                                                roomName
                                            )


                                    generatedRoomId =
                                        room.roomId


                                    roomError =
                                        null

                                } catch (
                                    e: Exception
                                ) {

                                    roomError =

                                        e.message
                                            ?: "Unable to create Room."
                                }
                            },


                            onContinueToRoom = {

                                if (
                                    activeRoom !=
                                    null
                                ) {

                                    radioState =
                                        RadioState.READY


                                    screen =
                                        AppScreen.ROOM_SESSION
                                }
                            }
                        )
                    }


                    // =================================================
                    // JOIN ROOM
                    // =================================================

                    AppScreen.JOIN_ROOM -> {

                        JoinRoomScreen(

                            onBackClick = {

                                isJoiningRoom =
                                    false


                                roomError =
                                    null


                                screen =
                                    AppScreen.ROOM
                            },


                            isJoining =
                                isJoiningRoom,


                            errorMessage =
                                roomError,


                            onJoinRoom = {
                                    roomId ->


                                if (
                                    !isJoiningRoom
                                ) {

                                    isJoiningRoom =
                                        true


                                    roomError =
                                        null


                                    communicationScope.launch {


                                        val result =

                                            roomMeshManager
                                                .joinRoom(
                                                    roomId
                                                )


                                        isJoiningRoom =
                                            false


                                        if (
                                            result.success
                                        ) {

                                            radioState =
                                                RadioState.READY


                                            screen =
                                                AppScreen.ROOM_SESSION

                                        } else {

                                            roomError =
                                                result.error
                                        }
                                    }
                                }
                            }
                        )
                    }


                    // =================================================
                    // ROOM SESSION
                    // =================================================

                    AppScreen.ROOM_SESSION -> {


                        val room =
                            activeRoom


                        if (
                            room == null
                        ) {

                            LaunchedEffect(Unit) {

                                screen =
                                    AppScreen.ROOM
                            }

                        } else {

                            RoomSessionScreen(

                                roomId =
                                    room.roomId,


                                roomName =
                                    room.roomName,


                                participantCount =
                                    room.participantCount,


                                meshPeerCount =
                                    connectedPeers.size,


                                state =
                                    radioState,


                                speakingLanguage =
                                    speakingLanguage
                                        .displayName,


                                listeningLanguage =
                                    listeningLanguage
                                        .displayName,


                                onSpeakingLanguageChange = {

                                    speakingLanguage =

                                        languageFromName(
                                            it
                                        )
                                },


                                onListeningLanguageChange = {

                                    listeningLanguage =

                                        languageFromName(
                                            it
                                        )
                                },


                                onStartTalking = {

                                    if (

                                        radioState ==
                                        RadioState.READY
                                    ) {

                                        radioState =
                                            RadioState.LISTENING


                                        startRecording(

                                            roomMode =
                                                true,

                                            peerAddress =
                                                null,

                                            roomId =
                                                room.roomId,

                                            speakingLanguage =
                                                speakingLanguage,

                                            listeningLanguage =
                                                listeningLanguage,

                                            onStateChange = {

                                                radioState =
                                                    it
                                            },

                                            onHistoryChanged = {

                                                refreshHistory()
                                            }
                                        )
                                    }
                                },


                                onStopTalking = {

                                    stopRecording()
                                },


                                onHistoryClick = {

                                    returnScreen =
                                        AppScreen.ROOM_SESSION


                                    refreshHistory()


                                    screen =
                                        AppScreen.HISTORY
                                },


                                onSettingsClick = {

                                    returnScreen =
                                        AppScreen.ROOM_SESSION


                                    screen =
                                        AppScreen.SETTINGS
                                },


                                onAboutClick = {

                                    aboutReturnScreen =
                                        AppScreen.ROOM_SESSION


                                    screen =
                                        AppScreen.ABOUT
                                },


                                onLeaveRoom = {

                                    communicationScope.launch {

                                        roomMeshManager
                                            .leaveRoom()


                                        radioState =
                                            RadioState.READY


                                        screen =
                                            AppScreen.ROOM
                                    }
                                }
                            )
                        }
                    }


                    // =================================================
                    // SETTINGS
                    // =================================================

                    AppScreen.SETTINGS -> {

                        SettingsScreen(

                            connectedDeviceName =
                                selectedDeviceName,


                            onBackClick = {

                                screen =
                                    returnScreen
                            },


                            onConnectedDeviceClick = {

                                pairedDeviceNames =
                                    getPairedDeviceNames()


                                screen =
                                    AppScreen.CONNECT
                            },


                            onMicrophonePermissionClick = {

                                requestMicrophonePermission()
                            },


                            onBluetoothPermissionClick = {

                                requestBluetoothPermissions()
                            },


                            onAboutClick = {

                                aboutReturnScreen =
                                    AppScreen.SETTINGS


                                screen =
                                    AppScreen.ABOUT
                            }
                        )
                    }


                    // =================================================
                    // ABOUT
                    // =================================================

                    AppScreen.ABOUT -> {

                        AboutScreen(

                            onBack = {

                                screen =
                                    aboutReturnScreen
                            }
                        )
                    }
                }


                // =================================================
                // ANDROID BACK
                // =================================================

                BackHandler(

                    enabled =
                        screen !=
                                AppScreen.SPLASH

                ) {


                    when (screen) {


                        AppScreen.HISTORY -> {

                            screen =
                                returnScreen
                        }


                        AppScreen.SETTINGS -> {

                            screen =
                                returnScreen
                        }


                        AppScreen.ABOUT -> {

                            screen =
                                aboutReturnScreen
                        }


                        AppScreen.CREATE_ROOM,
                        AppScreen.JOIN_ROOM -> {

                            screen =
                                AppScreen.ROOM
                        }


                        AppScreen.ROOM_SESSION -> {

                            communicationScope.launch {

                                roomMeshManager
                                    .leaveRoom()


                                radioState =
                                    RadioState.READY


                                screen =
                                    AppScreen.ROOM
                            }
                        }


                        AppScreen.ROOM -> {

                            screen =
                                AppScreen.RADIO
                        }


                        AppScreen.CONNECTING -> {

                            screen =
                                AppScreen.CONNECT
                        }


                        AppScreen.RADIO -> {

                            /*
                             * Do not disconnect all mesh sockets merely
                             * because the user navigates back.
                             */

                            screen =
                                AppScreen.CONNECT
                        }


                        AppScreen.CONNECT -> {

                            screen =
                                AppScreen.PERMISSIONS
                        }


                        AppScreen.PERMISSIONS -> {

                            // Stay here.
                        }


                        AppScreen.SPLASH -> {

                            // Disabled above.
                        }
                    }
                }
            }
        }
    }


    // =============================================================
    // RECORD -> STT -> HISTORY -> TRANSPORT
    // =============================================================

    private fun startRecording(

        roomMode: Boolean,

        peerAddress: String?,

        roomId: String?,

        speakingLanguage: Language,

        listeningLanguage: Language,

        onStateChange:
            (RadioState) -> Unit,

        onHistoryChanged:
            () -> Unit

    ) {


        if (

            recordingJob
                ?.isActive ==
            true
        ) {

            return
        }


        recordingJob =

            communicationScope.launch(

                Dispatchers.Default
            ) {


                try {


                    // =================================================
                    // RECORD UNTIL RELEASE OR 15 SECONDS
                    // =================================================

                    val audio =

                        audioRecorder
                            .record(
                                15_000
                            )


                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        onStateChange(
                            RadioState.PROCESSING
                        )
                    }


                    // =================================================
                    // OFFLINE STT
                    // =================================================

                    val recognizedText =

                        sttEngine
                            .transcribe(
                                audio
                            )


                    if (
                        recognizedText
                            .isBlank()
                    ) {

                        Handler(
                            Looper.getMainLooper()
                        ).post {

                            onStateChange(
                                RadioState.READY
                            )
                        }


                        return@launch
                    }


                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        onStateChange(
                            RadioState.SENDING
                        )
                    }


                    // =================================================
                    // ROOM MESH BROADCAST
                    // =================================================

                    if (
                        roomMode
                    ) {


                        val message =

                            roomMeshManager
                                .broadcastText(

                                    languageCode =
                                        speakingLanguage.code,

                                    text =
                                        recognizedText
                                )


                        if (
                            message ==
                            null
                        ) {

                            throw IllegalStateException(
                                "No active Room."
                            )
                        }


                        textBackupStore.logText(

                            messageId =
                                message.id,

                            direction =
                                RecordingDirection.SENT,

                            speakingLanguage =
                                speakingLanguage.displayName,

                            listeningLanguage =
                                listeningLanguage.displayName,

                            originalText =
                                recognizedText,

                            translatedText =
                                "",

                            roomId =
                                roomId,

                            timestamp =
                                message.timestamp
                        )


                        // =================================================
                        // DIRECT ONE-TO-ONE
                        // =================================================

                    } else {


                        val address =

                            peerAddress
                                .orEmpty()


                        if (
                            address.isBlank()
                        ) {

                            throw IllegalStateException(
                                "No direct Bluetooth device selected."
                            )
                        }


                        val message =

                            Message(

                                id =
                                    "msg-" +
                                            UUID
                                                .randomUUID()
                                                .toString(),

                                type =
                                    Message.TYPE_NORMAL,

                                language =
                                    speakingLanguage.code,

                                timestamp =
                                    System.currentTimeMillis(),

                                text =
                                    recognizedText,

                                originNodeId =
                                    roomMeshManager
                                        .localNodeId,

                                ttl =
                                    0
                            )


                        val sent =

                            communicationService
                                .sendDirectMessage(

                                    peerAddress =
                                        address,

                                    message =
                                        message
                                )


                        if (
                            !sent
                        ) {

                            throw IllegalStateException(
                                "Text could not be delivered to the selected device."
                            )
                        }


                        textBackupStore.logText(

                            messageId =
                                message.id,

                            direction =
                                RecordingDirection.SENT,

                            speakingLanguage =
                                speakingLanguage.displayName,

                            listeningLanguage =
                                listeningLanguage.displayName,

                            originalText =
                                recognizedText,

                            translatedText =
                                "",

                            timestamp =
                                message.timestamp
                        )
                    }


                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        onHistoryChanged()


                        onStateChange(
                            RadioState.READY
                        )
                    }


                } catch (
                    e: Exception
                ) {


                    Log.e(

                        "RADIO",

                        "Speech pipeline failed",

                        e
                    )


                    Handler(
                        Looper.getMainLooper()
                    ).post {

                        onStateChange(
                            RadioState.ERROR
                        )
                    }
                }
            }
    }


    // =============================================================
    // RELEASE-TO-STOP PTT
    // =============================================================

    private fun stopRecording() {

        try {

            audioRecorder
                .stop()

        } catch (
            e: Exception
        ) {

            Log.w(

                "AUDIO",

                "Unable to stop recorder",

                e
            )
        }
    }


    // =============================================================
    // PERMISSION HELPERS
    // =============================================================

    private fun requestAllPermissions() {

        requestMicrophonePermission()


        requestBluetoothPermissions()
    }


    private fun requestMicrophonePermission() {

        if (

            ContextCompat
                .checkSelfPermission(

                    this,

                    Manifest.permission.RECORD_AUDIO

                ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            microphonePermissionLauncher
                .launch(
                    Manifest.permission.RECORD_AUDIO
                )
        }
    }


    private fun requestBluetoothPermissions() {

        val permissions =

            BluetoothPermissionHelper
                .getRequiredPermissions()


        val missing =

            permissions
                .filter {

                    ContextCompat
                        .checkSelfPermission(

                            this,

                            it

                        ) !=
                            PackageManager.PERMISSION_GRANTED
                }


        if (
            missing.isNotEmpty()
        ) {

            bluetoothPermissionLauncher
                .launch(
                    missing.toTypedArray()
                )

        } else {

            startBluetoothServer()
        }
    }


    // =============================================================
    // SERVER
    // =============================================================

    private fun startBluetoothServer() {

        try {

            if (

                BluetoothPermissionHelper
                    .hasRequiredPermissions(
                        applicationContext
                    )
            ) {

                communicationService
                    .startServer()
            }

        } catch (
            e: Exception
        ) {

            Log.e(

                "BLUETOOTH",

                "Unable to start Bluetooth mesh server",

                e
            )
        }
    }


    // =============================================================
    // PAIRED DEVICES
    // =============================================================

    private fun getPairedDeviceNames():
            List<String> {


        return try {

            BluetoothPermissionHelper
                .getPairedDevices()
                .mapNotNull {

                    try {

                        it.name

                    } catch (
                        _: SecurityException
                    ) {

                        null
                    }
                }
                .distinct()
                .sorted()

        } catch (
            e: Exception
        ) {

            Log.e(

                "BLUETOOTH",

                "Unable to read paired devices",

                e
            )


            emptyList()
        }
    }


    private fun findPairedDevice(
        deviceName: String
    ): BluetoothDevice? {


        return try {

            BluetoothPermissionHelper
                .getPairedDevices()
                .firstOrNull {

                    try {

                        it.name ==
                                deviceName

                    } catch (
                        _: SecurityException
                    ) {

                        false
                    }
                }

        } catch (
            e: Exception
        ) {

            Log.e(

                "BLUETOOTH",

                "Unable to resolve paired device",

                e
            )


            null
        }
    }


    // =============================================================
    // CLEANUP
    // =============================================================

    override fun onDestroy() {


        try {

            recordingJob
                ?.cancel()

        } catch (_: Exception) {
        }


        try {

            audioRecorder
                .stop()

        } catch (_: Exception) {
        }


        try {

            sttEngine
                .release()

        } catch (_: Exception) {
        }


        try {

            ttsEngine
                .release()

        } catch (_: Exception) {
        }


        try {

            roomMeshManager
                .release()

        } catch (_: Exception) {
        }


        try {

            communicationService
                .disconnect()

        } catch (_: Exception) {
        }


        communicationScope
            .cancel()


        super.onDestroy()
    }
}
