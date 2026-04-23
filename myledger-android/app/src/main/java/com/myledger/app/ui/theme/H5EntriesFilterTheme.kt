package com.myledger.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** H5 `.filter-select`：圆角 12px */
val H5EntriesFilterSelectShape = RoundedCornerShape(12.dp)

/** H5 `.search-shell`：圆角 14px */
val H5EntriesRemarkFieldShape = RoundedCornerShape(14.dp)

/** 流水筛选下拉项少，菜单不必过高（H5 原生 select 展开也较紧凑） */
val EntriesFilterDropdownMaxHeight = 200.dp

/** 接近 H5 filter-select 底：白叠浅灰 */
private val H5FilterFieldContainer = Color(0xFFF8FAFA)

@Composable
fun h5EntriesFilterTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary,
    focusedBorderColor = Primary.copy(alpha = 0.42f),
    unfocusedBorderColor = Line,
    disabledBorderColor = Line,
    focusedContainerColor = H5FilterFieldContainer,
    unfocusedContainerColor = H5FilterFieldContainer,
    disabledContainerColor = H5FilterFieldContainer,
    cursorColor = Primary,
    focusedLeadingIconColor = Muted.copy(alpha = 0.88f),
    unfocusedLeadingIconColor = Muted.copy(alpha = 0.88f),
    focusedTrailingIconColor = Muted,
    unfocusedTrailingIconColor = Muted,
    disabledTrailingIconColor = Muted,
    focusedPlaceholderColor = Muted.copy(alpha = 0.62f),
    unfocusedPlaceholderColor = Muted.copy(alpha = 0.62f),
)
