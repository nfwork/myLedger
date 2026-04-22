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

/** H5 规范中的阴影 (0 4px 24px rgb(15 23 42 / 0.06)) */
fun Modifier.h5Shadow(): Modifier = this.shadow(
    elevation = 8.dp, // 稍微大一点以模拟 24px 的模糊效果
    shape = RoundedCornerShape(RadiusCard),
    clip = false,
    spotColor = Color(0xFF0F172A).copy(alpha = 0.08f),
    ambientColor = Color.Transparent
)

/** H5 卡片样式 */
fun Modifier.h5Card(): Modifier = this
    .h5Shadow()
    .clip(RoundedCornerShape(RadiusCard))
    .background(Surface)
    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(RadiusCard)) // H5 有一个淡白边框
    .border(1.dp, Line.copy(alpha = 0.5f), RoundedCornerShape(RadiusCard)) // 叠一层淡淡的 Teal 边框

/** H5 页面根部外边距 */
val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
