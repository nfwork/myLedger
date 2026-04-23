package com.myledger.app.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * 支出/收入等分段控件点击反馈：主色水波纹，避免默认灰 ripple 与浅底（如 [Bg]、浅灰槽）糊成一团。
 */
fun Modifier.segmentToggleClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = Primary.copy(alpha = 0.38f)),
        onClick = onClick,
    )
}
