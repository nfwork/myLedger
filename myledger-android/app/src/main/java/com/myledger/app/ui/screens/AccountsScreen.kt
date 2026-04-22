package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
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
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "资金账户表示钱所在的位置（现金、银行卡、支付宝等）；记一笔必选账户。概览、统计、流水列表可通过顶部筛选按账户查看。",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(12.dp),
            fontSize = 13.sp,
            color = Muted,
            lineHeight = 19.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("新账户名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newSort,
                onValueChange = { newSort = it.filter { c -> c.isDigit() } },
                placeholder = { Text("排序（数字越小越靠前）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
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
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text("添加") }
        }
        if (loading) {
            Text("加载中…", color = Muted, modifier = Modifier.padding(16.dp))
        } else {
            rows.forEach { a ->
                val id = a.get("id").asLong
                val name = a.get("name")?.asString ?: ""
                val sort = a.get("sort_order")?.asInt ?: 0
                val def = isDefault(a)
                if (editId == id) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = editSort,
                            onValueChange = { editSort = it.filter { c -> c.isDigit() } },
                            placeholder = { Text("排序") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            ) { Text("保存") }
                            TextButton(onClick = { editId = null }) { Text("取消") }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .padding(14.dp),
                    ) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("排序 $sort", fontSize = 11.sp, color = Muted)
                            if (def) {
                                Text(
                                    "默认",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            if (!def) {
                                TextButton(
                                    onClick = {
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
                                    },
                                ) { Text("设为默认", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                            TextButton(onClick = { editId = id; editName = name; editSort = sort.toString() }) {
                                Text("改名", color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (!def) {
                                TextButton(
                                    onClick = {
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
                                    },
                                ) { Text("删除", color = Expense, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
            if (!loading && rows.isEmpty()) {
                Text("暂无资金账户", color = Muted, modifier = Modifier.padding(24.dp))
            }
        }
    }
}

private suspend fun refreshFilterAccounts() {
    // 与 H5 refreshFilterAccounts 一致：账户列表变化后校验筛选 ID
    val arr = AppServices.ledgerRepository.listAccounts().mapJsonObjects()
    val ids = arr.map { it.get("id").asLong }.toSet()
    val cur = AppServices.accountScopeStore.getScopeAccountId()
    if (cur != null && cur !in ids) {
        AppServices.accountScopeStore.setScopeAccountId(null)
    }
}
