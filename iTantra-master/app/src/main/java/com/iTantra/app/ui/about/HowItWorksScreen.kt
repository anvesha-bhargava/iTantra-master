package com.iTantra.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iTantra.app.ui.theme.ITantraBackground
import com.iTantra.app.ui.theme.ITantraGreen
import com.iTantra.app.ui.theme.ITantraSecondaryText
import com.iTantra.app.ui.theme.ITantraSurface
import com.iTantra.app.ui.theme.ITantraText

@Composable
fun HowItWorksScreen(
    onBack: () -> Unit
) {

    val steps =
        listOf(
            "1" to
                    "Speak — the microphone captures your voice locally.",
            "2" to
                    "STT — sherpa-ONNX converts the audio into text on-device.",
            "3" to
                    "Transmit — only lightweight UTF-8 text is sent over Bluetooth.",
            "4" to
                    "Mesh — Room packets can hop through nearby iTANTRA devices.",
            "5" to
                    "Receive — duplicate IDs and TTL prevent relay loops.",
            "6" to
                    "TTS — the receiver reconstructs speech locally using its voice model."
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ITantraBackground
                )
                .padding(
                    horizontal = 22.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick =
                    onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription =
                        "Back",
                    tint =
                        ITantraText
                )
            }

            Text(
                text =
                    "How It Works",
                color =
                    ITantraText,
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )

        Text(
            text =
                "Offline communication pipeline",
            color =
                ITantraGreen,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        Text(
            text =
                "Audio stays on the device. The network transports text packets instead of raw voice.",
            color =
                ITantraSecondaryText,
            fontSize =
                13.sp,
            lineHeight =
                19.sp
        )

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )

        steps.forEach {
                (number, description) ->

            Surface(
                color =
                    ITantraSurface,
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            number,
                        color =
                            ITantraGreen,
                        fontSize =
                            22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            description,
                        color =
                            ITantraText,
                        fontSize =
                            13.sp,
                        lineHeight =
                            19.sp,
                        modifier =
                            Modifier.padding(
                                start = 16.dp
                            )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }
    }
}

