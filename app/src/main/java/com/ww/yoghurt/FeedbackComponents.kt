package com.ww.yoghurt

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView

@Composable
fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current

    Button(
        onClick = {
            // Bypasses software limits for the heavy hardware motor
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun FeedbackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    IconButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}