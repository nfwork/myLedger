package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TextPrimary
import com.myledger.app.ui.theme.h5Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun loadAccountsRows(): List<JsonObject> =
    withContext(Dispatchers.IO) {
        AppServices.ledgerRepository.listAccounts().mapJsonObjects()
    }

@Composable
fun AccountsScreen(
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newName by remember { mutableStateOf("") }
    var newSort by remember { mutableStateOf("100") }
    var adding by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Long?>(null) }
    var editName by remember { mutableStateOf("") }
    var editSort by remember { mutableStateOf("100") }
    var saving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<JsonObject?>(null) }
    val scope = rememberCoroutineScope()

    fun isDefault(a: JsonObject): Boolean {
        val v = a.get("is_default") ?: a.get("isDefault") ?: return false
        return when {
            v.isJsonPrimitive && v.asBoolean -> true
            v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asInt == 1
            else -> false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        try {
            rows = loadAccountsRows()
        } catch (e: Exception) {
            onError(e.message ?: "加载失败")
            rows = emptyList()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // H5 AccountsView.vue .tip.card
        Text(
            "资金账户表示钱所在的位置（现金、银行卡、支付宝等）；记一笔必选账户。概览、统计、流水列表可通过顶部筛选按账户查看。",
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Muted,
        )

        // H5 .add.card：横向布局，更紧凑
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                H5CompactInputField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "新账户名称",
                    modifier = Modifier.fillMaxWidth(),
                )
                H5CompactInputField(
                    value = newSort,
                    onValueChange = { newSort = it.filter { c -> c.isDigit() } },
                    placeholder = "排序 (100)",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    adding = true
                    scope.launch {
                        try {
                            val so = newSort.toIntOrNull() ?: 100
                            withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.addAccount(newName.trim(), so)
                            }
                            newName = ""
                            newSort = "100"
                            onSuccess("已添加账户")
                            rows = loadAccountsRows()
                            withContext(Dispatchers.IO) { refreshFilterAccounts() }
                        } catch (e: Exception) {
                            onError(e.message ?: "添加失败")
                        } finally {
                            adding = false
                        }
                    }
                },
                enabled = !adding && newName.isNotBlank(),
                modifier = Modifier.height(86.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("添加", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }

        if (loading) {
            Text("加载中…", color = Muted, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
        } else if (rows.isEmpty()) {
            Text(
                "暂无资金账户",
                color = Muted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            // 优化为单个卡片内的列表样式
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .h5Card()
            ) {
                rows.forEachIndexed { index, a ->
                    val id = a.get("id").asLong
                    val name = a.get("name")?.asString ?: ""
                    val sort = a.get("sort_order")?.asInt ?: 0
                    val def = isDefault(a)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (editId == id) {
                            H5CompactInputField(
                                value = editName,
                                onValueChange = { editName = it },
                                placeholder = "账户名称",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            H5CompactInputField(
                                value = editSort,
                                onValueChange = { editSort = it.filter { c -> c.isDigit() } },
                                placeholder = "排序",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { editId = null },
                                    modifier = Modifier.height(34.dp).padding(end = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) { Text("取消", fontSize = 12.sp) }

                                Button(
                                    onClick = {
                                        if (editName.isBlank()) {
                                            onError("名称不能为空")
                                            return@Button
                                        }
                                        saving = true
                                        scope.launch {
                                            try {
                                                val so = editSort.toIntOrNull()
                                                withContext(Dispatchers.IO) {
                                                    AppServices.ledgerRepository.updateAccount(id, editName.trim(), so)
                                                }
                                                editId = null
                                                onSuccess("已保存")
                                                rows = loadAccountsRows()
                                                withContext(Dispatchers.IO) { refreshFilterAccounts() }
                                            } catch (e: Exception) {
                                                onError(e.message ?: "保存失败")
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    },
                                    enabled = !saving,
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                ) { Text("保存", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        if (def) {
                                            Text(
                                                "默认",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark,
                                                modifier = Modifier
                                                    .padding(start = 8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Primary.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text("排序 $sort", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!def) {
                                        ActionIconPill(
                                            label = "设默认",
                                            container = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                            content = Color(0xFF1D4ED8),
                                        ) {
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) { AppServices.ledgerRepository.setDefaultAccount(id) }
                                                    onSuccess("已设为默认账户")
                                                    rows = loadAccountsRows()
                                                    withContext(Dispatchers.IO) { refreshFilterAccounts() }
                                                } catch (e: Exception) {
                                                    onError(e.message ?: "操作失败")
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            editId = id
                                            editName = name
                                            editSort = sort.toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "改名", tint = Muted, modifier = Modifier.size(18.dp))
                                    }
                                    if (!def) {
                                        IconButton(
                                            onClick = {
                                                accountToDelete = a
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Expense.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (index < rows.size - 1) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = Line.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && accountToDelete != null) {
        val id = accountToDelete!!.get("id").asLong
        val name = accountToDelete!!.get("name")?.asString ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除账户「$name」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { AppServices.ledgerRepository.deleteAccount(id) }
                                onSuccess("已删除")
                                rows = loadAccountsRows()
                                withContext(Dispatchers.IO) { refreshFilterAccounts() }
                            } catch (e: Exception) {
                                onError(e.message ?: "删除失败")
                            }
                        }
                    }
                ) { Text("删除", color = Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ActionIconPill(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(label, color = content, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

private suspend fun refreshFilterAccounts() {
    val arr = AppServices.ledgerRepository.listAccounts().mapJsonObjects()
    val ids = arr.map { it.get("id").asLong }.toSet()
    val cur = AppServices.accountScopeStore.getScopeAccountId()
    if (cur != null && cur !in ids) {
        AppServices.accountScopeStore.setScopeAccountId(null)
    }
}
