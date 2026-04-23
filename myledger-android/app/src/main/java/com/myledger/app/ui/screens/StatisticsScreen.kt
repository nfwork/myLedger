package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.domain.buildMatrix
import com.myledger.app.domain.currentYearMonth
import com.myledger.app.domain.formatMoney
import com.myledger.app.domain.visibleMonthsForStats
import com.myledger.app.ui.theme.CompactSelectFieldTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuItemMinHeight
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Income
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TextPrimary
import com.myledger.app.ui.theme.H5CompactSelectField
import com.myledger.app.ui.theme.H5EntriesFilterSelectShape
import com.myledger.app.ui.theme.H5ExposedDropdownMenu
import com.myledger.app.ui.theme.h5Card
import com.myledger.app.ui.theme.segmentToggleClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onError: (String) -> Unit) {
    val cy = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var selectedYear by remember { mutableIntStateOf(cy) }
    var entryType by remember { mutableStateOf("expense") }
    var scopeAccountId by remember {
        mutableStateOf(AppServices.accountScopeStore.getScopeAccountId())
    }
    var accounts by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var flatRows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var accMenu by remember { mutableStateOf(false) }
    val tableHScroll = rememberScrollState()

    LaunchedEffect(selectedYear, entryType, scopeAccountId) {
        loading = true
        try {
            val accList = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listAccounts().mapJsonObjects()
            }
            accounts = accList.map { it.get("id").asLong to (it.get("name")?.asString ?: "") }
            val months = visibleMonthsForStats(selectedYear)
            if (months.isEmpty()) {
                flatRows = emptyList()
            } else {
                val aid = scopeAccountId
                val arr = withContext(Dispatchers.IO) {
                    AppServices.ledgerRepository.matrixByMonthCategory(
                        months.first(),
                        months.last(),
                        entryType,
                        aid,
                    ).mapJsonObjects()
                }
                flatRows = arr
            }
        } catch (e: Exception) {
            onError(e.message ?: "加载失败")
            flatRows = emptyList()
        } finally {
            loading = false
        }
    }

    val months = visibleMonthsForStats(selectedYear)
    val matrix = remember(flatRows, months, entryType) { buildMatrix(flatRows, months) }
    val numColor = if (entryType == "expense") Expense else Income
    val curYm = currentYearMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(14.dp),
        ) {
            Text("月 × 分类（自然年）", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimaryDark)
            Spacer(Modifier.height(6.dp))
            Text(
                "行为月份、列为分类；按自然年汇总，默认当前年。查看今年时只展示到本月，往年仍为全年 12 个月。切换类型查看支出或收入分布。",
                fontSize = 12.sp,
                color = Muted,
                lineHeight = 18.sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("资金账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
                ExposedDropdownMenuBox(expanded = accMenu, onExpandedChange = { accMenu = it }, modifier = Modifier.weight(1f)) {
                    H5CompactSelectField(
                        value = if (scopeAccountId == null) "全部账户" else accounts.find { it.first == scopeAccountId }?.second ?: "全部账户",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    H5ExposedDropdownMenu(
                        expanded = accMenu,
                        onDismissRequest = { accMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部账户", style = CompactSelectMenuItemTextStyle) },
                            onClick = {
                                scopeAccountId = null
                                AppServices.accountScopeStore.setScopeAccountId(null)
                                accMenu = false
                            },
                            contentPadding = CompactSelectMenuItemPadding,
                            modifier = Modifier.heightIn(min = CompactSelectMenuItemMinHeight)
                        )
                        accounts.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name, style = CompactSelectMenuItemTextStyle) },
                                onClick = {
                                    scopeAccountId = id
                                    AppServices.accountScopeStore.setScopeAccountId(id)
                                    accMenu = false
                                },
                                contentPadding = CompactSelectMenuItemPadding,
                                modifier = Modifier.heightIn(min = CompactSelectMenuItemMinHeight)
                            )
                        }
                    }
                }
            }
            // 与 H5 StatisticsView.vue .type-tabs 一致：浅灰轨道 + 选中白底轻阴影
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.05f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf("expense" to "支出", "income" to "收入").forEach { (v, label) ->
                    val selected = entryType == v
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .then(
                                if (selected) {
                                    Modifier
                                        .shadow(1.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
                                        .background(Surface)
                                } else {
                                    Modifier.background(Color.Transparent)
                                },
                            )
                            .segmentToggleClickable { entryType = v }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (selected) PrimaryDark else Muted,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { selectedYear-- },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark.copy(alpha = 0.1f), contentColor = PrimaryDark),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一年")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("统计区间", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Text("${selectedYear}年", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Button(
                    onClick = { selectedYear++ },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark.copy(alpha = 0.1f), contentColor = PrimaryDark),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一年")
                }
            }
        }

        if (loading) {
            Text("加载中…", color = Muted, modifier = Modifier.padding(16.dp))
        } else if (matrix.cols.isEmpty()) {
            Text(
                "该自然年暂无${if (entryType == "expense") "支出" else "收入"}数据",
                color = Muted,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            // 与 H5 表格一致：左侧月份列固定宽 + 右侧整块横向滚动；每行固定高度，避免与锁定列错位、金额换行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .h5Card(),
            ) {
                Column(
                    modifier = Modifier
                        .width(StatsMonthColW)
                        .background(StatsTableHeadBg),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(StatsHeaderRowH)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "月份",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Muted,
                        )
                    }
                    matrix.rows.forEachIndexed { ri, row ->
                        val isCur = selectedYear == cy && row.ym == curYm
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(StatsBodyRowH)
                                .background(statsRowBg(isCur, ri % 2 == 1))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                row.monthLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(StatsFooterRowH)
                            .background(StatsTableFootBg)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "分类合计",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(tableHScroll)
                        .padding(end = 12.dp),
                ) {
                    Column {
                        // 与 H5 .matrix thead th 一致：整行表头底 #f8fafc，合计列再由 .total-col 叠 #f0f9f8
                        Row(
                            Modifier
                                .height(StatsHeaderRowH)
                                .background(StatsTableHeadBg),
                        ) {
                            matrix.cols.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .width(StatsCellW)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        c.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(StatsTotalColW)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    "合计",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            }
                        }
                        matrix.rows.forEachIndexed { ri, row ->
                            val isCur = selectedYear == cy && row.ym == curYm
                            Row(
                                modifier = Modifier
                                    .height(StatsBodyRowH)
                                    .background(statsRowBg(isCur, ri % 2 == 1)),
                            ) {
                                row.amounts.forEach { v ->
                                    Box(
                                        modifier = Modifier
                                            .width(StatsCellW)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Text(
                                            cellMoney(v),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (v == 0.0) Color(0xFF94A3B8) else numColor,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .width(StatsTotalColW)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        cellMoney(row.rowTotal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (row.rowTotal == 0.0) Color(0xFF94A3B8) else numColor,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .height(StatsFooterRowH)
                                .background(StatsTableFootBg),
                        ) {
                            matrix.colTotals.forEach { v ->
                                Box(
                                    modifier = Modifier
                                        .width(StatsCellW)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        cellMoney(v),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = numColor,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(StatsTotalColW)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    cellMoney(matrix.grand),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = numColor,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val StatsMonthColW = 84.dp
private val StatsCellW = 84.dp
private val StatsTotalColW = 84.dp
private val StatsHeaderRowH = 48.dp
private val StatsBodyRowH = 44.dp
private val StatsFooterRowH = 52.dp
private val StatsTableHeadBg = Color(0xFFF8FAFC)
private val StatsTableFootBg = Color(0xFFE6F4F2)

private fun statsRowBg(isCurrent: Boolean, zebra: Boolean): Color = when {
    isCurrent -> Color(0xFFDAF2EC)
    zebra -> Color(0xFFF1F5F9)
    else -> Surface
}

private fun cellMoney(n: Double): String {
    if (n == 0.0) return "—"
    return formatMoney(n)
}
