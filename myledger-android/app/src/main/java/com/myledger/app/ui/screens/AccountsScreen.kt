package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardR = 16.dp

private suspend fun loadAccountsRows(): List<JsonObject> =
    withContext(Dispatchers.IO) {
        AppServices.ledgerRepository.listAccounts().mapJsonObjects()
    }

private fun Modifier.h5Card(): Modifier =
    this
        .shadow(4.dp, RoundedCornerShape(CardR), spotColor = Color(0x0F0F172A).copy(alpha = 0.06f), ambientColor = Color.Transparent)
        .clip(RoundedCornerShape(CardR))
        .background(Surface)
        .border(1.dp, Line, RoundedCornerShape(CardR))

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

        // H5 .add.card：纵向字段 + 全宽主按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("新账户名称", color = Muted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newSort,
                onValueChange = { newSort = it.filter { c -> c.isDigit() } },
                placeholder = { Text("排序（数字越小越靠前）", color = Muted) },
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text("添加", fontWeight = FontWeight.ExtraBold) }
        }

        if (loading) {
            Text("加载中…", color = Muted, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
        } else {
            rows.forEach { a ->
                val id = a.get("id").asLong
                val name = a.get("name")?.asString ?: ""
                val sort = a.get("sort_order")?.asInt ?: 0
                val def = isDefault(a)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .h5Card()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (editId == id) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = editSort,
                            onValueChange = { editSort = it.filter { c -> c.isDigit() } },
                            placeholder = { Text("排序", color = Muted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.align(Alignment.CenterEnd),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
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
                            ) { Text("保存", fontWeight = FontWeight.ExtraBold) }
                            OutlinedButton(onClick = { editId = null }) { Text("取消") }
                            }
                        }
                    } else {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Text("排序 $sort", fontSize = 11.sp, color = Muted)
                            if (def) {
                                Text(
                                    "默认",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Box(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.align(Alignment.CenterEnd),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                            if (!def) {
                                ActionPill(
                                    label = "设为默认",
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
                            ActionPill(
                                label = "改名",
                                container = Primary.copy(alpha = 0.1f),
                                content = PrimaryDark,
                            ) {
                                editId = id
                                editName = name
                                editSort = sort.toString()
                            }
                            if (!def) {
                                ActionPill(
                                    label = "删除",
                                    container = Expense.copy(alpha = 0.08f),
                                    content = Expense,
                                ) {
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
                            }
                            }
                        }
                    }
                }
            }
            if (!loading && rows.isEmpty()) {
                Text(
                    "暂无资金账户",
                    color = Muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, color = content, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
