package com.myledger.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** H5 规范中的圆角 */
val RadiusCard = 16.dp
val RadiusButton = 12.dp
val RadiusInput = 12.dp

/** H5 规范中的阴影 (更柔和的现代阴影) */
fun Modifier.h5Shadow(): Modifier = this.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(RadiusCard),
    clip = false,
    spotColor = Color(0xFF0F172A).copy(alpha = 0.05f),
    ambientColor = Color.Transparent
)

/** H5 卡片样式 */
fun Modifier.h5Card(): Modifier = this
    .h5Shadow()
    .clip(RoundedCornerShape(RadiusCard))
    .background(Surface)
    .border(0.5.dp, Color(0xFF0D9488).copy(alpha = 0.08f), RoundedCornerShape(RadiusCard))


/** H5 页面根部外边距 */
val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
