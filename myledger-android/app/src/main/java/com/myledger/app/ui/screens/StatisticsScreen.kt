package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedTextField
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
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Income
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
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
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
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
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("资金账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
                ExposedDropdownMenuBox(expanded = accMenu, onExpandedChange = { accMenu = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = if (scopeAccountId == null) "全部账户" else accounts.find { it.first == scopeAccountId }?.second ?: "全部账户",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        textStyle = CompactSelectFieldTextStyle,
                    )
                    ExposedDropdownMenu(expanded = accMenu, onDismissRequest = { accMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("全部账户", style = CompactSelectMenuItemTextStyle) },
                            onClick = {
                                scopeAccountId = null
                                AppServices.accountScopeStore.setScopeAccountId(null)
                                accMenu = false
                            },
                            contentPadding = CompactSelectMenuItemPadding,
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
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("expense" to "支出", "income" to "收入").forEach { (v, label) ->
                    Button(
                        onClick = { entryType = v },
                        colors = if (entryType == v) {
                            ButtonDefaults.buttonColors(containerColor = Surface, contentColor = PrimaryDark)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = Color(0x0F0F172A).copy(alpha = 0.05f), contentColor = Muted)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(label, fontWeight = FontWeight.ExtraBold) }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface),
            ) {
                Column(
                    modifier = Modifier
                        .width(76.dp)
                        .background(Color(0xFFF8FAFC)),
                ) {
                    Text(
                        "月份",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.padding(10.dp),
                    )
                    matrix.rows.forEachIndexed { ri, row ->
                        val isCur = selectedYear == cy && row.ym == curYm
                        Text(
                            row.monthLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        isCur -> Color(0xFFDAF2EC)
                                        ri % 2 == 1 -> Color(0xFFF1F5F9)
                                        else -> Surface
                                    },
                                )
                                .padding(10.dp),
                        )
                    }
                    Text(
                        "分类合计",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE6F4F2))
                            .padding(10.dp),
                    )
                }
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    Column {
                        Row {
                            matrix.cols.forEach { c ->
                                Text(
                                    c.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(64.dp).padding(8.dp),
                                )
                            }
                            Text(
                                "合计",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp).padding(8.dp).background(Color(0xFFF0F9F8)),
                                textAlign = TextAlign.End,
                            )
                        }
                        matrix.rows.forEachIndexed { ri, row ->
                            val isCur = selectedYear == cy && row.ym == curYm
                            Row(
                                modifier = Modifier.background(
                                    when {
                                        isCur -> Color(0xFFDAF2EC)
                                        ri % 2 == 1 -> Color(0xFFF1F5F9)
                                        else -> Surface
                                    },
                                ),
                            ) {
                                row.amounts.forEach { v ->
                                    Text(
                                        cellMoney(v),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (v == 0.0) Color(0xFF94A3B8) else numColor,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(64.dp).padding(vertical = 10.dp, horizontal = 8.dp),
                                    )
                                }
                                Text(
                                    cellMoney(row.rowTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = numColor,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(72.dp).padding(10.dp).background(Color(0xFFF0F9F8)),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.background(Color(0xFFE6F4F2)),
                        ) {
                            matrix.colTotals.forEach { v ->
                                Text(
                                    cellMoney(v),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = numColor,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(64.dp).padding(12.dp, 10.dp),
                                )
                            }
                            Text(
                                cellMoney(matrix.grand),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = numColor,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(72.dp).padding(12.dp, 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cellMoney(n: Double): String {
    if (n == 0.0) return "—"
    return formatMoney(n)
}
