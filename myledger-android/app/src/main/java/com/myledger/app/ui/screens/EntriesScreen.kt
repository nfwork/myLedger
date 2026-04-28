package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.myledger.app.domain.currentYearMonth
import com.myledger.app.domain.formatDateDisplay
import com.myledger.app.domain.formatMoney
import com.myledger.app.domain.formatYearMonthLabel
import com.myledger.app.domain.shiftYearMonth
import com.myledger.app.ui.theme.CompactSelectFieldTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuItemMinHeight
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.ui.theme.EntriesFilterDropdownMaxHeight
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.H5CompactSelectField
import com.myledger.app.ui.theme.H5EntriesFilterSelectShape
import com.myledger.app.ui.theme.H5EntriesRemarkFieldShape
import com.myledger.app.ui.theme.Income
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.TextPrimary
import com.myledger.app.ui.theme.h5Card
import com.myledger.app.ui.theme.H5ExposedDropdownMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 20

private data class EntryRowUiModel(
    val id: Long,
    val initial: String,
    val category: String,
    val sub: String,
    val amountText: String,
    val isIncome: Boolean,
)

private data class EntryDayGroupUiModel(
    val dateKey: String,
    val dateLabel: String,
    val incomeText: String?,
    val expenseText: String?,
    val rows: List<EntryRowUiModel>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesScreen(
    onOpenEntry: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    var yearMonth by remember { mutableStateOf(currentYearMonth()) }
    var entryType by remember { mutableStateOf("") }
    var scopeAccountId by remember {
        mutableStateOf(AppServices.accountScopeStore.getScopeAccountId())
    }
    var accounts by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var remarkKeyword by remember { mutableStateOf("") }
    var debouncedRemark by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var accMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(remarkKeyword) {
        delay(350)
        debouncedRemark = remarkKeyword.trim()
    }

    LaunchedEffect(Unit) {
        try {
            val accList = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listAccounts().mapJsonObjects()
            }
            accounts = accList.map { (it.optLong("id") ?: 0L) to (it.optString("name") ?: "") }
        } catch (e: Exception) {
            onError(e.message ?: "账户加载失败")
        }
    }

    LaunchedEffect(yearMonth, entryType, scopeAccountId, debouncedRemark) {
        loading = true
        try {
            val aid = scopeAccountId
            val (arr, n) = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.entryListPage(
                    buildMap {
                        put("year_month", yearMonth)
                        put("start", 0)
                        put("limit", PAGE_SIZE)
                        if (entryType.isNotBlank()) put("entry_type", entryType)
                        val kw = debouncedRemark
                        if (kw.isNotBlank()) put("remark_keyword", kw)
                        if (aid != null) put("account_id", aid)
                    },
                )
            }
            rows = arr.mapJsonObjects()
            total = n
        } catch (e: Exception) {
            onError(e.message ?: "加载失败")
            rows = emptyList()
            total = 0
        } finally {
            loading = false
        }
    }

    fun queryBody(start: Int) = buildMap<String, Any> {
        put("year_month", yearMonth)
        put("start", start)
        put("limit", PAGE_SIZE)
        if (entryType.isNotBlank()) put("entry_type", entryType)
        val kw = debouncedRemark
        if (kw.isNotBlank()) put("remark_keyword", kw)
        val aid = scopeAccountId
        if (aid != null) put("account_id", aid)
    }
    val groupedRows = remember(rows) { rows.toEntryDayGroups() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部“筛选区”：使用极浅的主题背景色，与 TopBar 衔接
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary.copy(alpha = 0.04f))
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Text(formatYearMonthLabel(yearMonth), fontWeight = FontWeight.Black, fontSize = 17.sp, color = PrimaryDark)
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

            // 综合筛选卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .h5Card()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("类型", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }, modifier = Modifier.weight(1f)) {
                        val typeLabel = when (entryType) {
                            "income" -> "收入"
                            "expense" -> "支出"
                            else -> "全部"
                        }
                        H5CompactSelectField(
                            value = typeLabel,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        H5ExposedDropdownMenu(
                            expanded = typeMenu,
                            onDismissRequest = { typeMenu = false },
                            maxHeight = EntriesFilterDropdownMaxHeight,
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = ""; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                                modifier = Modifier.heightIn(min = CompactSelectMenuItemMinHeight)
                            )
                            DropdownMenuItem(
                                text = { Text("收入", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = "income"; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                                modifier = Modifier.heightIn(min = CompactSelectMenuItemMinHeight)
                            )
                            DropdownMenuItem(
                                text = { Text("支出", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = "expense"; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                                modifier = Modifier.heightIn(min = CompactSelectMenuItemMinHeight)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    ExposedDropdownMenuBox(expanded = accMenu, onExpandedChange = { accMenu = it }, modifier = Modifier.weight(1f)) {
                        H5CompactSelectField(
                            value = if (scopeAccountId == null) "全部" else accounts.find { it.first == scopeAccountId }?.second ?: "全部",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        H5ExposedDropdownMenu(
                            expanded = accMenu,
                            onDismissRequest = { accMenu = false },
                            maxHeight = EntriesFilterDropdownMaxHeight,
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部", style = CompactSelectMenuItemTextStyle) },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("备注", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    H5CompactInputField(
                        value = remarkKeyword,
                        onValueChange = { remarkKeyword = it },
                        placeholder = "输入关键词搜索…",
                        modifier = Modifier.weight(1f),
                        shape = H5EntriesRemarkFieldShape,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Muted.copy(alpha = 0.6f),
                            )
                        },
                        trailingIcon = if (remarkKeyword.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { remarkKeyword = "" },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "清空",
                                        modifier = Modifier.size(14.dp),
                                        tint = Muted,
                                    )
                                }
                            }
                        } else null
                    )
                }
            }
        }

        // 列表区
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                } else if (rows.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("暂无流水", color = Muted, fontSize = 15.sp)
                        }
                    }
                } else {
                    groupedRows.forEach { group ->
                        item(key = "group-${group.dateKey}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .h5Card()
                            ) {
                                // 分组标题
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A).copy(alpha = 0.02f))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(group.dateLabel, fontWeight = FontWeight.Black, fontSize = 14.sp, color = TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        group.incomeText?.let { incomeText ->
                                            Text(incomeText, color = Income, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        group.expenseText?.let { expenseText ->
                                            Text(expenseText, color = Expense, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                group.rows.forEachIndexed { idx, row ->
                                    EntryListRow(row, onClick = { onOpenEntry(row.id) })
                                    if (idx < group.rows.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = Line.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (total > 0) {
                        item {
                            if (rows.size < total) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !loadingMore) {
                                            loadingMore = true
                                            scope.launch {
                                                try {
                                                    val (arr, n) = withContext(Dispatchers.IO) {
                                                        AppServices.ledgerRepository.entryListPage(queryBody(rows.size))
                                                    }
                                                    rows = rows + arr.mapJsonObjects()
                                                    total = n
                                                } catch (e: Exception) {
                                                    onError(e.message ?: "加载更多失败")
                                                } finally {
                                                    loadingMore = false
                                                }
                                            }
                                        }
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                                        Text(" 加载中…", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("查看更多 (已加载 ${rows.size}/$total)", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    "已显示全部 $total 条流水",
                                    color = Muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 顶部半透明过渡：遮挡列表滚入顶部的硬边界
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
private fun EntryListRow(row: EntryRowUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (row.isIncome) Income.copy(alpha = 0.1f) else Expense.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                row.initial,
                color = if (row.isIncome) Income else Expense,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                row.category,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            if (row.sub.isNotEmpty()) {
                Text(
                    row.sub,
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            row.amountText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (row.isIncome) Income else Expense,
            textAlign = TextAlign.End
        )
    }
}

private fun List<JsonObject>.toEntryDayGroups(): List<EntryDayGroupUiModel> =
    groupBy { it.get("entry_date")?.asStringOrNull()?.take(10) ?: "" }
        .map { (date, dayRows) ->
            var dayIncome = 0.0
            var dayExpense = 0.0
            val rowModels = dayRows.map { row ->
                val isIncome = row.get("entry_type")?.asStringOrNull() == "income"
                val amount = row.optDouble("amount") ?: 0.0
                if (isIncome) {
                    dayIncome += amount
                } else {
                    dayExpense += amount
                }
                val category = row.get("category_name")?.asStringOrNull() ?: "—"
                val acc = row.get("account_name")?.asStringOrNull()
                val remark = row.get("remark")?.asStringOrNull()
                val sub = listOfNotNull(
                    acc.takeIf { !it.isNullOrBlank() },
                    remark.takeIf { !it.isNullOrBlank() }
                ).joinToString(" · ")
                EntryRowUiModel(
                    id = row.optLong("id") ?: 0L,
                    initial = category.take(1),
                    category = category,
                    sub = sub,
                    amountText = (if (isIncome) "+" else "−") + formatMoney(amount),
                    isIncome = isIncome,
                )
            }
            EntryDayGroupUiModel(
                dateKey = date,
                dateLabel = formatDateDisplay(date),
                incomeText = dayIncome.takeIf { it > 0 }?.let { "收 ${formatMoney(it)}" },
                expenseText = dayExpense.takeIf { it > 0 }?.let { "支 ${formatMoney(it)}" },
                rows = rowModels,
            )
        }
