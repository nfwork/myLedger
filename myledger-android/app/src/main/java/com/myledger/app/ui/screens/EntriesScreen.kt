package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.domain.currentYearMonth
import com.myledger.app.domain.formatDateDisplay
import com.myledger.app.domain.formatMoney
import com.myledger.app.domain.shiftYearMonth
import com.myledger.app.ui.theme.CompactSelectFieldTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuMaxHeight
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Income
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 20

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

    LaunchedEffect(yearMonth, entryType, scopeAccountId, debouncedRemark) {
        loading = true
        try {
            val accList = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listAccounts().mapJsonObjects()
            }
            accounts = accList.map { it.get("id").asLong to (it.get("name")?.asString ?: "") }
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

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { yearMonth = shiftYearMonth(yearMonth, -1) },
                    modifier = Modifier.padding(end = 4.dp).size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark.copy(alpha = 0.1f), contentColor = PrimaryDark),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                }
                Text(yearMonth, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Button(
                    onClick = { yearMonth = shiftYearMonth(yearMonth, 1) },
                    modifier = Modifier.padding(start = 4.dp).size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark.copy(alpha = 0.1f), contentColor = PrimaryDark),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("类型", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 10.dp))
                    ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }, modifier = Modifier.weight(1f)) {
                        val typeLabel = when (entryType) {
                            "income" -> "收入"
                            "expense" -> "支出"
                            else -> "全部"
                        }
                        OutlinedTextField(
                            value = typeLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            textStyle = CompactSelectFieldTextStyle,
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenu,
                            onDismissRequest = { typeMenu = false },
                            modifier = Modifier.heightIn(max = CompactSelectMenuMaxHeight),
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = ""; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                            )
                            DropdownMenuItem(
                                text = { Text("收入", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = "income"; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                            )
                            DropdownMenuItem(
                                text = { Text("支出", style = CompactSelectMenuItemTextStyle) },
                                onClick = { entryType = "expense"; typeMenu = false },
                                contentPadding = CompactSelectMenuItemPadding,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 10.dp))
                    ExposedDropdownMenuBox(expanded = accMenu, onExpandedChange = { accMenu = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (scopeAccountId == null) "全部" else accounts.find { it.first == scopeAccountId }?.second ?: "全部",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            textStyle = CompactSelectFieldTextStyle,
                        )
                        ExposedDropdownMenu(
                            expanded = accMenu,
                            onDismissRequest = { accMenu = false },
                            modifier = Modifier.heightIn(max = CompactSelectMenuMaxHeight),
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部", style = CompactSelectMenuItemTextStyle) },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("备注", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 10.dp))
                    OutlinedTextField(
                        value = remarkKeyword,
                        onValueChange = { remarkKeyword = it },
                        placeholder = { Text("搜索备注…", fontSize = 14.sp, lineHeight = 17.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = CompactSelectFieldTextStyle.copy(fontWeight = FontWeight.Normal),
                    )
                }
                Text("仅在当前月份流水中，对备注做模糊匹配", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(start = 38.dp))
            }
        }

        if (loading) {
            item { Text("加载中…", color = Muted, modifier = Modifier.padding(16.dp)) }
        } else if (rows.isEmpty()) {
            item { Text("暂无流水", color = Muted, modifier = Modifier.padding(32.dp).fillMaxWidth(), fontSize = 15.sp) }
        } else {
            items(rows, key = { it.get("id").asLong }) { row ->
                EntryListCard(row, onClick = { onOpenEntry(row.get("id").asLong) })
            }
            if (total > 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("已显示 ${rows.size} / $total 条", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (rows.size < total) {
                            OutlinedButton(
                                onClick = {
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
                                },
                                enabled = !loadingMore,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Text(if (loadingMore) "加载中…" else "加载更多", color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Text("已加载全部", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryListCard(row: JsonObject, onClick: () -> Unit) {
    val income = row.get("entry_type")?.asString == "income"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (income) "收入" else "支出",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (income) Income else Expense,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (income) Income.copy(alpha = 0.12f) else Expense.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Text(
                    row.get("category_name")?.asString ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            val acc = row.get("account_name")?.asString
            val remark = row.get("remark")?.asString
            val sub = buildString {
                append(formatDateDisplay(row.get("entry_date")?.asString))
                if (!acc.isNullOrBlank()) append(" · ").append(acc)
                if (!remark.isNullOrBlank()) append(" · ").append(remark)
            }
            Text(sub, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        val amt = row.get("amount")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
        Text(
            (if (income) "+" else "−") + formatMoney(amt),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = if (income) Income else Expense,
        )
    }
}
