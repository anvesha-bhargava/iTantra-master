package com.iTantra.app.ui.radio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.iTantra.app.ui.theme.ITantraBackground
import com.iTantra.app.ui.theme.ITantraBorder
import com.iTantra.app.ui.theme.ITantraDarkGreen
import com.iTantra.app.ui.theme.ITantraError
import com.iTantra.app.ui.theme.ITantraGreen
import com.iTantra.app.ui.theme.ITantraLightGreen
import com.iTantra.app.ui.theme.ITantraSecondaryText
import com.iTantra.app.ui.theme.ITantraSuccess
import com.iTantra.app.ui.theme.ITantraSurface
import com.iTantra.app.ui.theme.ITantraText


// =============================================================
// SUPPORTED UI LANGUAGES
// =============================================================

private val supportedLanguages =

    listOf(

        "Hindi",

        "English",

        "Tamil",

        "Telugu",

        "Bengali",

        "Gujarati",

        "Malayalam",

        "Marathi",

        "Odia",

        "Kannada"
    )


// =============================================================
// MAIN RADIO SCREEN
// =============================================================

@Composable
fun MainRadioScreen(

    state: RadioState,

    connectedDevice: String,

    connectedDeviceCount: Int = 1,

    speakingLanguage: String,

    listeningLanguage: String,

    onSpeakingLanguageChange:
        (String) -> Unit,

    onListeningLanguageChange:
        (String) -> Unit,

    onRoomClick:
        () -> Unit,

    onHistoryClick:
        () -> Unit,

    onSettingsClick:
        () -> Unit,

    onAboutClick:
        () -> Unit,

    onStartTalking:
        () -> Unit,

    onStopTalking:
        () -> Unit
) {


    var drawerOpen by remember {

        mutableStateOf(
            false
        )
    }


    Box(

        modifier =

            Modifier
                .fillMaxSize()
                .background(
                    ITantraBackground
                )
    ) {


        Column(

            modifier =

                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 22.dp
                    )
        ) {


            Spacer(

                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            // =====================================================
            // HEADER
            // =====================================================

            RadioHeader(

                connectedDeviceCount =
                    connectedDeviceCount,

                onMenuClick = {

                    drawerOpen =
                        true
                }
            )


            Spacer(

                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            /*
             * Optional direct-device name.
             *
             * The count pill above represents all directly-connected
             * mesh neighbours. This label identifies the device selected
             * for one-to-one mode.
             */

            if (
                connectedDevice.isNotBlank()
            ) {

                Text(

                    text =
                        "Direct: $connectedDevice",

                    color =
                        ITantraSecondaryText,

                    fontSize =
                        10.sp,

                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            // =====================================================
            // TRANSMISSION DIAGRAM
            // =====================================================

            TransmissionDiagram(

                speakingLanguage =
                    speakingLanguage,

                listeningLanguage =
                    listeningLanguage,

                speakerActive =

                    state ==
                            RadioState.LISTENING,

                receiverActive =

                    state ==
                            RadioState.RECEIVING ||

                            state ==
                            RadioState.PLAYING
            )


            Spacer(

                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            // =====================================================
            // LANGUAGES
            // =====================================================

            LanguageRow(

                speakingLanguage =
                    speakingLanguage,

                listeningLanguage =
                    listeningLanguage,

                onSpeakingLanguageChange =
                    onSpeakingLanguageChange,

                onListeningLanguageChange =
                    onListeningLanguageChange,

                onSwapLanguages = {


                    val oldSpeaking =
                        speakingLanguage


                    onSpeakingLanguageChange(
                        listeningLanguage
                    )


                    onListeningLanguageChange(
                        oldSpeaking
                    )
                }
            )


            Spacer(

                modifier =
                    Modifier.weight(
                        0.55f
                    )
            )


            // =====================================================
            // PTT / STATE
            // =====================================================

            StateVisual(

                state =
                    state,

                onStartTalking =
                    onStartTalking,

                onStopTalking =
                    onStopTalking
            )


            Spacer(

                modifier =
                    Modifier.weight(
                        0.45f
                    )
            )


            // =====================================================
            // FOOTER
            // =====================================================

            Text(

                text =
                    "Only lightweight text is transmitted",

                color =
                    ITantraGreen,

                fontSize =
                    12.sp,

                modifier =
                    Modifier.align(
                        Alignment.CenterHorizontally
                    )
            )


            Spacer(

                modifier =
                    Modifier.height(
                        28.dp
                    )
            )
        }


        // =========================================================
        // DRAWER
        // =========================================================

        if (
            drawerOpen
        ) {


            Box(

                modifier =

                    Modifier
                        .fillMaxSize()
                        .background(

                            Color.Black.copy(
                                alpha = 0.25f
                            )
                        )
                        .clickable {

                            drawerOpen =
                                false
                        }
            )


            NavigationDrawer(

                onClose = {

                    drawerOpen =
                        false
                },

                onRoomClick = {

                    drawerOpen =
                        false

                    onRoomClick()
                },

                onSettingsClick = {

                    drawerOpen =
                        false

                    onSettingsClick()
                },

                onHistoryClick = {

                    drawerOpen =
                        false

                    onHistoryClick()
                },

                onAboutClick = {

                    drawerOpen =
                        false

                    onAboutClick()
                }
            )
        }
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun RadioHeader(

    connectedDeviceCount: Int,

    onMenuClick:
        () -> Unit
) {


    Column(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            IconButton(

                onClick =
                    onMenuClick,

                modifier =
                    Modifier.size(
                        44.dp
                    )
            ) {


                Icon(

                    imageVector =
                        Icons.Default.Menu,

                    contentDescription =
                        "Menu",

                    tint =
                        ITantraGreen,

                    modifier =
                        Modifier.size(
                            28.dp
                        )
                )
            }


            Spacer(

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            // =====================================================
            // LIVE DIRECT PEER COUNT
            // =====================================================

            Surface(

                color =
                    ITantraLightGreen,

                shape =
                    RoundedCornerShape(
                        100.dp
                    )
            ) {


                Row(

                    modifier =
                        Modifier.padding(

                            horizontal =
                                10.dp,

                            vertical =
                                7.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    Box(

                        modifier =

                            Modifier
                                .size(
                                    7.dp
                                )
                                .background(

                                    color =

                                        if (
                                            connectedDeviceCount >
                                            0
                                        ) {

                                            ITantraSuccess

                                        } else {

                                            ITantraSecondaryText
                                        },

                                    shape =
                                        CircleShape
                                )
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )


                    Text(

                        text =

                            "$connectedDeviceCount " +

                                    if (
                                        connectedDeviceCount ==
                                        1
                                    ) {

                                        "Device"

                                    } else {

                                        "Devices"
                                    },

                        color =
                            ITantraDarkGreen,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(

            text =
                "iTANTRA",

            color =
                ITantraText,

            fontSize =
                26.sp,

            fontWeight =
                FontWeight.Medium
        )


        Spacer(

            modifier =
                Modifier.height(
                    3.dp
                )
        )


        Text(

            text =
                "Offline Multilingual Communication",

            color =
                ITantraSecondaryText,

            fontSize =
                11.sp
        )
    }
}


// =============================================================
// DRAWER
// =============================================================

@Composable
private fun NavigationDrawer(

    onClose:
        () -> Unit,

    onRoomClick:
        () -> Unit,

    onSettingsClick:
        () -> Unit,

    onHistoryClick:
        () -> Unit,

    onAboutClick:
        () -> Unit
) {


    Box(

        modifier =

            Modifier
                .fillMaxHeight()
                .width(
                    290.dp
                )
                .background(

                    ITantraBackground,

                    RoundedCornerShape(

                        topEnd =
                            24.dp,

                        bottomEnd =
                            24.dp
                    )
                )
                .padding(

                    horizontal =
                        20.dp,

                    vertical =
                        24.dp
                )
    ) {


        Column(

            modifier =
                Modifier.fillMaxSize()
        ) {


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Text(

                    text =
                        "iTANTRA",

                    color =
                        ITantraGreen,

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Medium,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                IconButton(

                    onClick =
                        onClose
                ) {


                    Icon(

                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription =
                            "Close menu",

                        tint =
                            ITantraText
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        30.dp
                    )
            )


            DrawerItem(

                icon =
                    Icons.Default.Groups,

                title =
                    "Room",

                onClick =
                    onRoomClick
            )


            DrawerItem(

                icon =
                    Icons.Default.Settings,

                title =
                    "Settings",

                onClick =
                    onSettingsClick
            )


            DrawerItem(

                icon =
                    Icons.Default.History,

                title =
                    "History",

                onClick =
                    onHistoryClick
            )


            DrawerItem(

                icon =
                    Icons.Default.Info,

                title =
                    "About App",

                onClick =
                    onAboutClick
            )
        }
    }
}


@Composable
private fun DrawerItem(

    icon:
    ImageVector,

    title:
    String,

    onClick:
        () -> Unit
) {


    Row(

        modifier =

            Modifier
                .fillMaxWidth()
                .clickable {

                    onClick()
                }
                .padding(

                    horizontal =
                        10.dp,

                    vertical =
                        15.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        Box(

            modifier =

                Modifier
                    .size(
                        42.dp
                    )
                    .background(

                        ITantraLightGreen,

                        CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {


            Icon(

                imageVector =
                    icon,

                contentDescription =
                    title,

                tint =
                    ITantraGreen,

                modifier =
                    Modifier.size(
                        21.dp
                    )
            )
        }


        Spacer(

            modifier =
                Modifier.width(
                    14.dp
                )
        )


        Text(

            text =
                title,

            color =
                ITantraText,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}


// =============================================================
// TOP DIAGRAM
// =============================================================

@Composable
private fun TransmissionDiagram(

    speakingLanguage: String,

    listeningLanguage: String,

    speakerActive: Boolean,

    receiverActive: Boolean
) {


    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {


        DiagramDevice(

            label =
                "YOU",

            icon =
                Icons.Default.Mic,

            language =
                speakingLanguage,

            isActive =
                speakerActive
        )


        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Icon(

                imageVector =
                    Icons.Default.Description,

                contentDescription =
                    null,

                tint =
                    ITantraGreen
            )


            Spacer(

                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Icon(

                imageVector =
                    Icons.Default.Bluetooth,

                contentDescription =
                    null,

                tint =
                    ITantraSecondaryText
            )
        }


        DiagramDevice(

            label =
                "RECEIVER",

            icon =
                Icons.Default.VolumeUp,

            language =
                listeningLanguage,

            isActive =
                receiverActive
        )
    }
}


@Composable
private fun DiagramDevice(

    label:
    String,

    icon:
    ImageVector,

    language:
    String,

    isActive:
    Boolean
) {


    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Text(

            text =
                label,

            color =

                if (
                    isActive
                ) {

                    ITantraGreen

                } else {

                    ITantraSecondaryText
                },

            fontSize =
                11.sp,

            fontWeight =

                if (
                    isActive
                ) {

                    FontWeight.Bold

                } else {

                    FontWeight.Normal
                }
        )


        Spacer(

            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Box(

            modifier =

                Modifier
                    .size(

                        width =
                            58.dp,

                        height =
                            74.dp
                    )
                    .background(

                        color =

                            if (
                                isActive
                            ) {

                                ITantraLightGreen

                            } else {

                                ITantraSurface
                            },

                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    )
                    .border(

                        width =

                            if (
                                isActive
                            ) {

                                2.dp

                            } else {

                                1.dp
                            },

                        color =

                            if (
                                isActive
                            ) {

                                ITantraGreen

                            } else {

                                ITantraBorder
                            },

                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    ),

            contentAlignment =
                Alignment.Center
        ) {


            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    ITantraGreen,

                modifier =
                    Modifier.size(
                        28.dp
                    )
            )
        }


        Spacer(

            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(

            text =
                language,

            color =

                if (
                    isActive
                ) {

                    ITantraGreen

                } else {

                    ITantraSecondaryText
                },

            fontSize =
                12.sp,

            fontWeight =

                if (
                    isActive
                ) {

                    FontWeight.SemiBold

                } else {

                    FontWeight.Normal
                }
        )
    }
}


// =============================================================
// LANGUAGE ROW
// =============================================================

@Composable
private fun LanguageRow(

    speakingLanguage:
    String,

    listeningLanguage:
    String,

    onSpeakingLanguageChange:
        (String) -> Unit,

    onListeningLanguageChange:
        (String) -> Unit,

    onSwapLanguages:
        () -> Unit
) {


    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        LanguageDropdownCard(

            heading =
                "I SPEAK",

            language =
                speakingLanguage,

            onLanguageSelected =
                onSpeakingLanguageChange,

            modifier =
                Modifier.weight(
                    1f
                )
        )


        Box(

            modifier =

                Modifier
                    .padding(
                        horizontal = 10.dp
                    )
                    .size(
                        40.dp
                    )
                    .background(

                        ITantraLightGreen,

                        CircleShape
                    )
                    .clickable {

                        onSwapLanguages()
                    },

            contentAlignment =
                Alignment.Center
        ) {


            Icon(

                imageVector =
                    Icons.Default.SwapHoriz,

                contentDescription =
                    "Swap languages",

                tint =
                    ITantraGreen
            )
        }


        LanguageDropdownCard(

            heading =
                "YOU HEAR",

            language =
                listeningLanguage,

            onLanguageSelected =
                onListeningLanguageChange,

            modifier =
                Modifier.weight(
                    1f
                )
        )
    }
}


@Composable
private fun LanguageDropdownCard(

    heading:
    String,

    language:
    String,

    onLanguageSelected:
        (String) -> Unit,

    modifier:
    Modifier =
        Modifier
) {


    var expanded by remember {

        mutableStateOf(
            false
        )
    }


    Box(

        modifier =
            modifier
    ) {


        Column(

            modifier =

                Modifier
                    .fillMaxWidth()
                    .background(

                        ITantraSurface,

                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .border(

                        width =
                            1.dp,

                        color =
                            ITantraBorder,

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    )
                    .clickable {

                        expanded =
                            true
                    }
                    .padding(
                        13.dp
                    )
        ) {


            Text(

                text =
                    heading,

                color =
                    ITantraSecondaryText,

                fontSize =
                    10.sp
            )


            Spacer(

                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Text(

                    text =
                        language,

                    color =
                        ITantraText,

                    fontSize =
                        14.sp,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Icon(

                    imageVector =
                        Icons.Default.KeyboardArrowDown,

                    contentDescription =
                        "Choose language",

                    tint =
                        ITantraGreen
                )
            }
        }


        DropdownMenu(

            expanded =
                expanded,

            onDismissRequest = {

                expanded =
                    false
            }
        ) {


            supportedLanguages
                .forEach {
                        option: String ->


                    DropdownMenuItem(

                        text = {

                            Text(
                                option
                            )
                        },

                        onClick = {

                            expanded =
                                false


                            onLanguageSelected(
                                option
                            )
                        }
                    )
                }
        }
    }
}


// =============================================================
// RADIO STATE / PTT
// =============================================================

@Composable
private fun StateVisual(

    state:
    RadioState,

    onStartTalking:
        () -> Unit,

    onStopTalking:
        () -> Unit
) {


    val busy =

        state !=
                RadioState.READY &&

                state !=
                RadioState.LISTENING


    val infiniteTransition =

        rememberInfiniteTransition(
            label =
                "pttPulse"
        )


    val pulse by

    infiniteTransition
        .animateFloat(

            initialValue =
                0.35f,

            targetValue =
                1f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(
                            850
                        ),

                    repeatMode =
                        RepeatMode.Reverse
                ),

            label =
                "pulse"
        )


    Column(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Box(

            modifier =
                Modifier.size(
                    166.dp
                ),

            contentAlignment =
                Alignment.Center
        ) {


            if (
                state ==
                RadioState.LISTENING
            ) {


                Canvas(

                    modifier =
                        Modifier.fillMaxSize()
                ) {


                    drawCircle(

                        color =
                            ITantraGreen.copy(
                                alpha =
                                    0.15f *
                                            pulse
                            ),

                        radius =
                            size.minDimension *
                                    0.48f
                    )


                    drawCircle(

                        color =
                            ITantraGreen.copy(
                                alpha =
                                    0.35f
                            ),

                        radius =
                            size.minDimension *
                                    0.43f,

                        style =
                            Stroke(
                                width =
                                    2.dp.toPx()
                            )
                    )
                }
            }


            Box(

                modifier =

                    Modifier
                        .size(
                            126.dp
                        )
                        .background(

                            color =

                                if (
                                    state ==
                                    RadioState.ERROR
                                ) {

                                    ITantraError

                                } else if (
                                    state ==
                                    RadioState.LISTENING
                                ) {

                                    ITantraDarkGreen

                                } else {

                                    ITantraGreen
                                },

                            shape =
                                CircleShape
                        )
                        .pointerInput(
                            busy,
                            state
                        ) {


                            detectTapGestures(

                                onPress = {


                                    if (
                                        !busy
                                    ) {


                                        onStartTalking()


                                        try {

                                            tryAwaitRelease()

                                        } finally {

                                            onStopTalking()
                                        }
                                    }
                                }
                            )
                        },

                contentAlignment =
                    Alignment.Center
            ) {


                when (state) {


                    RadioState.READY -> {


                        Icon(

                            imageVector =
                                Icons.Default.Mic,

                            contentDescription =
                                "Hold to talk",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    50.dp
                                )
                        )
                    }


                    RadioState.LISTENING -> {


                        Icon(

                            imageVector =
                                Icons.Default.GraphicEq,

                            contentDescription =
                                "Listening",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    52.dp
                                )
                        )
                    }


                    RadioState.PROCESSING -> {


                        CircularProgressIndicator(

                            color =
                                Color.White,

                            strokeWidth =
                                4.dp,

                            modifier =
                                Modifier.size(
                                    46.dp
                                )
                        )
                    }


                    RadioState.SENDING -> {


                        Icon(

                            imageVector =
                                Icons.Default.Send,

                            contentDescription =
                                "Sending",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    48.dp
                                )
                        )
                    }


                    RadioState.RECEIVING -> {


                        Icon(

                            imageVector =
                                Icons.Default.Download,

                            contentDescription =
                                "Receiving",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    48.dp
                                )
                        )
                    }


                    RadioState.PLAYING -> {


                        Icon(

                            imageVector =
                                Icons.Default.VolumeUp,

                            contentDescription =
                                "Playing",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    48.dp
                                )
                        )
                    }


                    RadioState.ERROR -> {


                        Icon(

                            imageVector =
                                Icons.Default.ErrorOutline,

                            contentDescription =
                                "Error",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    48.dp
                                )
                        )
                    }
                }
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Text(

            text =

                when (state) {


                    RadioState.READY ->

                        "Ready"


                    RadioState.LISTENING ->

                        "Listening"


                    RadioState.PROCESSING ->

                        "Processing"


                    RadioState.SENDING ->

                        "Sending"


                    RadioState.RECEIVING ->

                        "Receiving"


                    RadioState.PLAYING ->

                        "Playing"


                    RadioState.ERROR ->

                        "Error"
                },

            color =

                if (
                    state ==
                    RadioState.ERROR
                ) {

                    ITantraError

                } else {

                    ITantraText
                },

            fontSize =
                17.sp,

            fontWeight =
                FontWeight.SemiBold
        )


        Spacer(

            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(

            text =

                when (state) {


                    RadioState.READY ->

                        "Hold the microphone to speak"


                    RadioState.LISTENING ->

                        "Release to send the recognised text"


                    RadioState.PROCESSING ->

                        "Converting speech to text offline"


                    RadioState.SENDING ->

                        "Transmitting lightweight text"


                    RadioState.RECEIVING ->

                        "Text received from another device"


                    RadioState.PLAYING ->

                        "Playing received speech locally"


                    RadioState.ERROR ->

                        "Check permissions, model and connection"
                },

            color =
                ITantraSecondaryText,

            fontSize =
                12.sp
        )
    }
}
