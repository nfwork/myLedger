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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.data.remote.mapJsonObjects
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CategoriesScreen(
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    var tab by remember { mutableStateOf("expense") }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newName by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab) {
        loading = true
        try {
            val arr = withContext(Dispatchers.IO) {
                AppServices.ledgerRepository.listCategories(tab).mapJsonObjects()
            }
            rows = arr
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("expense" to "支出", "income" to "收入").forEach { (v, label) ->
                val sel = tab == v
                Button(
                    onClick = { tab = v },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sel) Primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (sel) com.myledger.app.ui.theme.PrimaryDark else Muted,
                    ),
                ) { Text(label, fontWeight = FontWeight.ExtraBold) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("新分类名称") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    adding = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.addCategory(newName.trim(), tab, 100)
                            }
                            newName = ""
                            onSuccess("已添加")
                            val arr = withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.listCategories(tab).mapJsonObjects()
                            }
                            rows = arr
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
        } else if (rows.isEmpty()) {
            Text("暂无分类", color = Muted, modifier = Modifier.padding(32.dp))
        } else {
            rows.forEach { c ->
                val id = c.get("id").asLong
                val name = c.get("name")?.asString ?: ""
                val sort = c.get("sort_order")?.asInt ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("排序 $sort", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        AppServices.ledgerRepository.deleteCategory(id)
                                    }
                                    onSuccess("已删除")
                                    val arr = withContext(Dispatchers.IO) {
                                        AppServices.ledgerRepository.listCategories(tab).mapJsonObjects()
                                    }
                                    rows = arr
                                } catch (e: Exception) {
                                    onError(e.message ?: "删除失败")
                                }
                            }
                        },
                    ) {
                        Text("删除", color = Expense, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
