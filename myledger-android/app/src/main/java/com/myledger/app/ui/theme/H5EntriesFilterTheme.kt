package com.myledger.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** H5 `.filter-select`：圆角 12px */
val H5EntriesFilterSelectShape = RoundedCornerShape(12.dp)

/** H5 `.search-shell`：圆角 14px */
val H5EntriesRemarkFieldShape = RoundedCornerShape(14.dp)

/** 流水筛选下拉项少，菜单不必过高（H5 原生 select 展开也较紧凑） */
val EntriesFilterDropdownMaxHeight = 200.dp

/** 接近 H5 filter-select 底：白叠浅灰 */
private val H5FilterFieldContainer = Color(0xFFF8FAFA)

/**
 * 自定义紧凑型选择框，解决 OutlinedTextField 默认内边距过大导致在低高度下文本截断的问题。
 * 默认高度 40dp，文本垂直居中。
 */
@Composable
fun H5CompactSelectField(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = H5EntriesFilterSelectShape,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(H5FilterFieldContainer, shape)
            .border(1.dp, Line, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = CompactSelectFieldTextStyle,
                        color = Muted.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = value,
                        style = CompactSelectFieldTextStyle,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingIcon != null) {
                Box(modifier = Modifier.padding(start = 4.dp)) { trailingIcon() }
            }
        }
    }
}

/**
 * 自定义紧凑型输入框，用于流水筛选页的备注搜索。
 */
@Composable
fun H5CompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = H5EntriesFilterSelectShape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .background(H5FilterFieldContainer, shape)
            .border(1.dp, Line, shape),
        textStyle = CompactSelectFieldTextStyle.copy(color = TextPrimary),
        singleLine = true,
        cursorBrush = SolidColor(Primary),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = CompactSelectFieldTextStyle,
                            color = Muted.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 4.dp)) { trailingIcon() }
                }
            }
        }
    )
}

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
