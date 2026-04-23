package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.ui.theme.CompactSelectFieldTextStyle
import com.myledger.app.ui.theme.CompactSelectMenuItemPadding
import com.myledger.app.ui.theme.CompactSelectMenuItemTextStyle
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.ui.theme.Bg
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.H5EntriesFilterSelectShape
import com.myledger.app.ui.theme.H5ExposedDropdownMenu
import com.myledger.app.ui.theme.h5Card
import com.myledger.app.ui.theme.h5EntriesFilterTextFieldColors
import com.myledger.app.ui.theme.segmentToggleClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryFormScreen(
    entryId: Long?,
    onDone: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    val isEdit = entryId != null
    var booting by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var accounts by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var entryType by remember { mutableStateOf("expense") }
    var amount by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(todayIso()) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var remark by remember { mutableStateOf("") }
    var accMenu by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val formScroll = rememberScrollState()

    fun isDefaultAccount(a: JsonObject): Boolean {
        val v = a.get("is_default") ?: a.get("isDefault") ?: return false
        return when {
            v.isJsonPrimitive && v.asBoolean -> true
            v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asInt == 1
            else -> false
        }
    }

    LaunchedEffect(entryId) {
        booting = true
        err = null
        try {
            val accs = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listAccounts().mapJsonObjects()
            }
            accounts = accs
            if (isEdit && entryId != null) {
                val row = withContext(Dispatchers.IO) {
                    AppServices.ledgerRepository.entryById(entryId)
                } ?: throw Exception("记录不存在")
                entryType = row.get("entry_type")?.asString ?: "expense"
                amount = row.get("amount")?.asString ?: ""
                entryDate = row.get("entry_date")?.asString?.take(10) ?: todayIso()
                accountId = row.get("account_id")?.asLong
                categoryId = row.get("category_id")?.asLong
                remark = row.get("remark")?.asString ?: ""
            } else {
                val def = accs.firstOrNull { isDefaultAccount(it) } ?: accs.firstOrNull()
                accountId = def?.get("id")?.asLong
            }
            val cats = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listCategories(entryType).mapJsonObjects()
            }
            categories = cats
            if (categoryId == null || cats.none { it.get("id").asLong == categoryId }) {
                categoryId = cats.firstOrNull()?.get("id")?.asLong
            }
        } catch (e: Exception) {
            err = e.message ?: "加载失败"
            onError(err!!)
        } finally {
            booting = false
        }
    }

    fun switchEntryType(v: String) {
        if (entryType == v) return
        scope.launch {
            entryType = v
            try {
                val cats = withContext(Dispatchers.IO) {
                    AppServices.ledgerRepository.listCategories(v).mapJsonObjects()
                }
                categories = cats
                categoryId = cats.firstOrNull { it.get("id").asLong == categoryId }?.get("id")?.asLong
                    ?: cats.firstOrNull()?.get("id")?.asLong
            } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(formScroll)
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (booting) {
            Text("加载中…", color = Muted, modifier = Modifier.padding(16.dp))
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 与 H5 EntryFormView.vue .toggle 一致：独立描边按钮，选中时主色描边 + 浅主色底
            Text("类型", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val shape = RoundedCornerShape(12.dp)
                listOf("expense" to "支出", "income" to "收入").forEach { (v, label) ->
                    val selected = entryType == v
                    val borderColor = if (selected) Primary.copy(alpha = 0.45f) else Line
                    val segmentBg = if (selected) Primary.copy(alpha = 0.12f) else Bg
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(segmentBg, shape)
                            .border(1.dp, borderColor, shape)
                            .segmentToggleClickable { switchEntryType(v) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (selected) PrimaryDark else Muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("金额") },
                placeholder = { Text("0.00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = entryDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("日期") },
                placeholder = { Text("选择日期") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = H5EntriesFilterSelectShape,
                colors = h5EntriesFilterTextFieldColors(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "选择日期",
                            tint = Muted,
                        )
                    }
                },
            )
            Text("资金账户", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(expanded = accMenu, onExpandedChange = { accMenu = it }, modifier = Modifier.fillMaxWidth()) {
                val a = accounts.find { it.get("id").asLong == accountId }
                val accLabel = a?.let {
                    val n = it.get("name")?.asString ?: ""
                    if (isDefaultAccount(it)) "$n（默认）" else n
                } ?: ""
                OutlinedTextField(
                    value = accLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    textStyle = CompactSelectFieldTextStyle,
                    shape = H5EntriesFilterSelectShape,
                    colors = h5EntriesFilterTextFieldColors(),
                )
                H5ExposedDropdownMenu(
                    expanded = accMenu,
                    onDismissRequest = { accMenu = false },
                ) {
                    accounts.forEach { ac ->
                        val id = ac.get("id").asLong
                        val n = ac.get("name")?.asString ?: ""
                        val label = if (isDefaultAccount(ac)) "$n（默认）" else n
                        DropdownMenuItem(
                            text = { Text(label, style = CompactSelectMenuItemTextStyle) },
                            onClick = {
                                accountId = id
                                accMenu = false
                            },
                            contentPadding = CompactSelectMenuItemPadding,
                        )
                    }
                }
            }
            Text("分类", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(expanded = catMenu, onExpandedChange = { catMenu = it }, modifier = Modifier.fillMaxWidth()) {
                val c = categories.find { it.get("id").asLong == categoryId }
                OutlinedTextField(
                    value = c?.get("name")?.asString ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    textStyle = CompactSelectFieldTextStyle,
                    shape = H5EntriesFilterSelectShape,
                    colors = h5EntriesFilterTextFieldColors(),
                )
                H5ExposedDropdownMenu(
                    expanded = catMenu,
                    onDismissRequest = { catMenu = false },
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.get("name")?.asString ?: "", style = CompactSelectMenuItemTextStyle) },
                            onClick = {
                                categoryId = cat.get("id").asLong
                                catMenu = false
                            },
                            contentPadding = CompactSelectMenuItemPadding,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text("备注（必填）") },
                placeholder = { Text("简要说明这笔账，便于日后按备注查找") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            err?.let { Text(it, color = Expense, fontSize = 13.sp) }
            Button(
                onClick = {
                    err = null
                    // 与表单自上而下顺序一致：金额 → 日期 → 账户 → 分类 → 备注
                    val amt = amount.toDoubleOrNull()
                    if (amount.isBlank()) {
                        err = "请输入金额"
                        onError(err!!)
                        return@Button
                    }
                    if (amt == null || amt < 0.01) {
                        err = "请输入有效金额"
                        onError(err!!)
                        return@Button
                    }
                    validateEntryDate(entryDate)?.let { msg ->
                        err = msg
                        onError(msg)
                        return@Button
                    }
                    if (accountId == null) {
                        err = "请选择资金账户"
                        onError(err!!)
                        return@Button
                    }
                    if (categoryId == null) {
                        err = "请选择分类"
                        onError(err!!)
                        return@Button
                    }
                    if (remark.trim().isEmpty()) {
                        err = "请填写备注"
                        onError(err!!)
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        try {
                            val body = mutableMapOf<String, Any>(
                                "account_id" to accountId!!,
                                "category_id" to categoryId!!,
                                "entry_type" to entryType,
                                "amount" to amt,
                                "entry_date" to entryDate,
                                "remark" to remark.trim(),
                            )
                            if (isEdit && entryId != null) {
                                body["id"] = entryId
                                withContext(Dispatchers.IO) { AppServices.ledgerRepository.updateEntry(body) }
                                onSuccess("已保存")
                            } else {
                                withContext(Dispatchers.IO) { AppServices.ledgerRepository.addEntry(body) }
                                onSuccess("已记账")
                            }
                            onDone()
                        } catch (e: Exception) {
                            err = e.message ?: "保存失败"
                            onError(err!!)
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(if (saving) "保存中…" else if (isEdit) "保存修改" else "保存")
            }
            if (isEdit) {
                OutlinedButton(
                    onClick = { showDelete = true },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Expense),
                ) {
                    Text("删除本条")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = isoDateToUtcStartMillis(entryDate) ?: isoDateToUtcStartMillis(todayIso())!!
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { entryDate = utcStartMillisToIsoDate(it) }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDelete && entryId != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("确认删除") },
            text = { Text("确定删除这条流水？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        saving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { AppServices.ledgerRepository.deleteEntry(entryId) }
                                onSuccess("已删除")
                                onDone()
                            } catch (e: Exception) {
                                onError(e.message ?: "删除失败")
                            } finally {
                                saving = false
                            }
                        }
                    },
                ) { Text("删除", color = Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            },
        )
    }
}

private fun todayIso(): String {
    val c = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/** 非 null 表示校验失败提示文案 */
private fun validateEntryDate(entryDate: String): String? {
    val s = entryDate.trim()
    if (s.isEmpty()) return "请填写日期"
    return try {
        LocalDate.parse(s)
        null
    } catch (_: Exception) {
        "请输入有效日期"
    }
}

/** Material DatePicker 使用 UTC 日界，与接口 YYYY-MM-DD 对齐 */
private fun isoDateToUtcStartMillis(iso: String): Long? =
    try {
        LocalDate.parse(iso.trim()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }

private fun utcStartMillisToIsoDate(millis: Long): String {
    val ld = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return String.format(Locale.US, "%04d-%02d-%02d", ld.year, ld.monthValue, ld.dayOfMonth)
}
