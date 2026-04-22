package com.myledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TealLight

/** 与 H5 TabBar.vue 中 .fab 的 margin-top: -1.35rem 对应，保证圆形按钮可向上“浮出”且不被裁剪 */
private val FabRise = 22.dp

private val FabSize = 48.dp

@Composable
fun MyBottomBar(
    currentRoute: String?,
    onDashboard: () -> Unit,
    onEntries: () -> Unit,
    onNewEntry: () -> Unit,
    onStats: () -> Unit,
    onProfile: () -> Unit,
) {
    val fabSelected = currentRoute == "entry_new" || (currentRoute?.startsWith("entry_edit/") == true)
    Column(modifier = Modifier.fillMaxWidth()) {
        // 参与布局：为中间 FAB 向上偏移留出空间，避免被底栏容器裁剪或与内容错误叠层
        Spacer(Modifier.height(FabRise))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RectangleShape,
                    ambientColor = Color(0x0F0D9488).copy(alpha = 0.08f),
                    spotColor = Color(0x0F0D9488).copy(alpha = 0.04f),
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Surface.copy(alpha = 0.92f),
                            Surface,
                        ),
                    ),
                ),
        ) {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Line, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TabIcon(
                        label = "概览",
                        icon = Icons.Default.Home,
                        selected = currentRoute == "dashboard",
                        onClick = onDashboard,
                    )
                    TabIcon(
                        label = "流水",
                        icon = Icons.Default.List,
                        selected = currentRoute == "entries",
                        onClick = onEntries,
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNewEntry),
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(y = -FabRise)
                                .size(FabSize)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    spotColor = Primary.copy(alpha = 0.45f),
                                    ambientColor = Primary.copy(alpha = 0.22f),
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            if (fabSelected) PrimaryDark else TealLight,
                                            Primary,
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "记账",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Text(
                            "记账",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (fabSelected) Primary else Muted,
                            modifier = Modifier.offset(y = (-8).dp),
                        )
                    }
                    TabIcon(
                        label = "统计",
                        icon = Icons.Default.BarChart,
                        selected = currentRoute == "stats",
                        onClick = onStats,
                    )
                    TabIcon(
                        label = "我的",
                        icon = Icons.Default.Person,
                        selected = currentRoute == "profile",
                        onClick = onProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabIcon(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (selected) Primary.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Primary else Muted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Primary else Muted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
