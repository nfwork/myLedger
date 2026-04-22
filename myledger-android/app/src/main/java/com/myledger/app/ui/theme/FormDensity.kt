package com.myledger.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 与 H5 `.scope-select` / `.filter-select` 接近：约 0.92rem 字号、半粗，行高收紧（当前 BOM 下 String 版 OutlinedTextField 无 contentPadding API）。
 */
val CompactSelectFieldTextStyle = TextStyle(
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
)

val CompactSelectMenuItemTextStyle = TextStyle(
    fontSize = 14.sp,
    lineHeight = 18.sp,
)

/** 下拉菜单项：比 Material3 默认更紧凑，避免列表项“过胖” */
val CompactSelectMenuItemPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
