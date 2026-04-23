package com.myledger.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 与流水页筛选一致：白底、浅线边框、无 tonal 叠色（避免发灰/发黑）、轻阴影。
 * [maxHeight] 流水等短列表可用 [EntriesFilterDropdownMaxHeight]，长列表用 [CompactSelectMenuMaxHeight]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.H5ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = CompactSelectMenuMaxHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(max = maxHeight),
        shape = H5EntriesFilterSelectShape,
        containerColor = Surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Line),
        content = content,
    )
}
