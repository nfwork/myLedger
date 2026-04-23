package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.asStringOrNull
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.data.remote.optDouble
import com.myledger.app.data.remote.optLong
import com.myledger.app.data.remote.optString
import com.myledger.app.domain.catAmount
import com.myledger.app.domain.currentYearMonth
import com.myledger.app.domain.formatDateDisplay
import com.myledger.app.domain.formatMoney
import com.myledger.app.domain.formatYearMonthLabel
import com.myledger.app.domain.shiftYearMonth
import com.myledger.app.ui.theme.CompactSelectFieldTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuItemMinHeight
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Income
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.TextPrimary
import com.myledger.app.ui.theme.H5CompactSelectField
import com.myledger.app.ui.theme.H5EntriesFilterSelectShape
import com.myledger.app.ui.theme.H5ExposedDropdownMenu
import com.myledger.app.ui.theme.h5Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TOP_CATEGORIES = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSeeAllEntries: () -> Unit,
    onError: (String) -> Unit,
) {
    var yearMonth by remember { mutableStateOf(currentYearMonth()) }
    var scopeAccountId by remember {
        mutableStateOf(AppServices.accountScopeStore.getScopeAccountId())
    }
    var accounts by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var totals by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var recent by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var expenseCats by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var incomeCats by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var accMenu by remember { mutableStateOf(false) }

    LaunchedEffect(yearMonth, scopeAccountId) {
        loading = true
        try {
            val accList = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listAccounts().mapJsonObjects()
            }
            accounts = accList.map {
                (it.optLong("id") ?: 0L) to (it.optString("name") ?: "")
            }
            val aid = scopeAccountId
            val tRow = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.monthTotals(yearMonth, aid)
            }
            val inc = tRow.get("income_total")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
            val exp = tRow.get("expense_total")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
            totals = inc to exp
            val recentArr = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.entryList(
                    buildMap {
                        put("year_month", yearMonth)
                        put("start", 0)
                        put("limit", 8)
                        if (aid != null) put("account_id", aid)
                    },
                ).mapJsonObjects()
            }
            val ex = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.categoryTotals(yearMonth, "expense", aid).mapJsonObjects()
            }
            val incat = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.categoryTotals(yearMonth, "income", aid).mapJsonObjects()
            }
            recent = recentArr
            expenseCats = ex
            incomeCats = incat
        } catch (e: Exception) {
            onError(e.message ?: "加载失败")
            totals = 0.0 to 0.0
            recent = emptyList()
            expenseCats = emptyList()
            incomeCats = emptyList()
        } finally {
            loading = false
        }
    }

    val balance = totals.first - totals.second
    val expenseMonthTotal = expenseCats.sumOf { catAmount(it) }
    val incomeMonthTotal = incomeCats.sumOf { catAmount(it) }
    fun barPct(part: Double, whole: Double): Float {
        if (whole <= 0 || part <= 0) return 0f
        return ((part / whole) * 100).toFloat().coerceIn(0f, 100f)
    }
    val expenseRows = expenseCats.filter { catAmount(it) > 0 }.take(TOP_CATEGORIES)
    val incomeRows = incomeCats.filter { catAmount(it) > 0 }.take(TOP_CATEGORIES)

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 顶部“设置与筛选”区域：使用极浅的主题背景色，与 TopBar 自然衔接
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary.copy(alpha = 0.04f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 月份选择器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .h5Card()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { yearMonth = shiftYearMonth(yearMonth, -1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.08f), contentColor = PrimaryDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月", modifier = Modifier.size(20.dp))
                }
                Text(
                    formatYearMonthLabel(yearMonth),
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = PrimaryDark
                )
                Button(
                    onClick = { yearMonth = shiftYearMonth(yearMonth, 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.08f), contentColor = PrimaryDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月", modifier = Modifier.size(20.dp))
                }
            }

            // 资金账户选择器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .h5Card()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("资金账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
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
        }

        // 内容区域
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), ambientColor = Color(0x2610B981), spotColor = Color(0x2610B981))
                                .background(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Text("本月收入", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(formatMoney(totals.first), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), ambientColor = Color(0x26F43F5E), spotColor = Color(0x26F43F5E))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Text("本月支出", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(formatMoney(totals.second), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .h5Card()
                            .background(Brush.horizontalGradient(
                                if (balance >= 0) listOf(Income.copy(alpha = 0.08f), Color.Transparent)
                                else listOf(Expense.copy(alpha = 0.08f), Color.Transparent)
                            ), RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("本月结余", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                formatMoney(balance),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (balance >= 0) Income else Expense,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (balance >= 0) Income.copy(alpha = 0.1f) else Expense.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (balance >= 0) "盈" else "亏", color = if (balance >= 0) Income else Expense, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (loading) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        if (expenseRows.isNotEmpty()) {
                            CategoryCard(
                                title = "支出分类",
                                hint = "占本月支出",
                                rows = expenseRows,
                                whole = expenseMonthTotal,
                                isIncome = false,
                                barPct = ::barPct,
                            )
                        }
                        if (incomeRows.isNotEmpty()) {
                            CategoryCard(
                                title = "收入分类",
                                hint = "占本月收入",
                                rows = incomeRows,
                                whole = incomeMonthTotal,
                                isIncome = true,
                                barPct = ::barPct,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("最近流水", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextPrimary)
                            Text(
                                "查看更多",
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSeeAllEntries() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .h5Card()
                                .padding(vertical = 4.dp),
                        ) {
                            if (recent.isEmpty()) {
                                Text(
                                    "本月还没有记账",
                                    modifier = Modifier.padding(32.dp).align(Alignment.CenterHorizontally),
                                    color = Muted,
                                    fontSize = 14.sp,
                                )
                            } else {
                                recent.forEachIndexed { idx, row ->
                                    RecentEntryRow(row)
                                    if (idx < recent.lastIndex) {
                                        Spacer(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .height(1.dp)
                                                .background(Line.copy(alpha = 0.3f)),
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                    }
                }

            // 顶部半透明过渡
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Primary.copy(alpha = 0.04f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    hint: String,
    rows: List<JsonObject>,
    whole: Double,
    isIncome: Boolean,
    barPct: (Double, Double) -> Float,
) {
    val barColor = if (isIncome) Brush.horizontalGradient(listOf(Color(0xFF34D399), Income)) else Brush.horizontalGradient(listOf(Color(0xFFFB7185), Expense))
    val amtColor = if (isIncome) Income else Expense
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .h5Card()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(hint, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        rows.forEach { c ->
            val name = c.get("category_name")?.asString ?: c.get("categoryName")?.asString ?: "—"
            val amt = catAmount(c)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(formatMoney(amt), color = amtColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            HorizontalBarTrack(barPct(amt, whole), barColor)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun HorizontalBarTrack(pct: Float, brush: Brush) {
    val f = (pct / 100f).coerceIn(0f, 1f).let { if (it > 0f && it < 0.02f) 0.02f else it }
    Box(
        Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x0F0F172A)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(brush),
        )
    }
}

@Composable
private fun RecentEntryRow(row: JsonObject) {
    val type = row.get("entry_type")?.asStringOrNull() ?: ""
    val income = type == "income"
    val cat = row.get("category_name")?.asStringOrNull() ?: "—"
    val date = formatDateDisplay(row.get("entry_date")?.asStringOrNull())
    val acc = row.get("account_name")?.asStringOrNull()
    val remark = row.get("remark")?.asStringOrNull()
    val amt = row.optDouble("amount") ?: 0.0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(cat, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val sub = buildString {
                append(date)
                if (!acc.isNullOrBlank()) append(" · ").append(acc)
                if (!remark.isNullOrBlank()) append(" · ").append(remark)
            }
            Text(sub, fontSize = 12.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(
            (if (income) "+" else "−") + formatMoney(amt),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (income) Income else Expense,
        )
    }
}
