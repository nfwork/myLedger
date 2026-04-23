package com.myledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TealLight

/** 与 H5 TabBar.vue .fab 的 margin-top: -1.35rem 接近；略收紧以少占纵向空间 */
private val FabRise = 18.dp

private val FabSize = 44.dp

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
    val density = LocalDensity.current
    val fabPx = with(density) { FabSize.toPx() }
    // 与 H5 .fab 的 linear-gradient(145deg, …) 接近：右上 → 左下
    val fabBrush = Brush.linearGradient(
        colors = listOf(
            if (fabSelected) PrimaryDark else TealLight,
            Primary,
        ),
        start = Offset(fabPx, 0f),
        end = Offset(0f, fabPx),
    )
    // 不再用顶部 Spacer 垫高整条底栏（会在 Scaffold 里形成一块与页面背景同色的「假条」，像把内容上顶且无悬浮感）。
    // FAB 仍用负 offset 浮出；graphicsLayer.clip=false 避免圆钮在底栏槽位内被裁掉，便于叠在列表上方。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lerp(Surface.copy(alpha = 0.92f), Primary, 0.025f),
                            Surface,
                        ),
                    ),
                ),
        ) {
            HorizontalDivider(color = Line, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TabIcon(
                    label = "首页",
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
                                elevation = 10.dp,
                                shape = CircleShape,
                                spotColor = Primary.copy(alpha = 0.45f),
                                ambientColor = Color.Transparent,
                            )
                            .background(fabBrush, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "记账",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        "记账",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fabSelected) Primary else Muted,
                        modifier = Modifier.offset(y = (-6).dp),
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
            .padding(vertical = 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Primary else Muted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Primary else Muted,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
