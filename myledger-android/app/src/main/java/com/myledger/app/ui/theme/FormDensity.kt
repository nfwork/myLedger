package com.myledger.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 与 H5 `.scope-select` / `.filter-select` 接近：约 0.92rem 字号、半粗，行高收紧。
 */
val CompactSelectFieldTextStyle = TextStyle(
    fontSize = 14.sp,
    lineHeight = 17.sp,
    fontWeight = FontWeight.SemiBold,
)

val CompactSelectMenuItemTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Normal,
)

/** 下拉菜单项：比 Material3 默认更紧凑 */
val CompactSelectMenuItemPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)

/** 账户等较长列表展开时的最大高度，超出部分在菜单内滚动 */
val CompactSelectMenuMaxHeight = 240.dp
