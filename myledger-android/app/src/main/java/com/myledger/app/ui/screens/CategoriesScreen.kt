package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TextPrimary
import com.myledger.app.ui.theme.h5Card
import com.myledger.app.ui.theme.segmentToggleClickable
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
    var editId by remember { mutableStateOf<Long?>(null) }
    var editName by remember { mutableStateOf("") }
    var editSort by remember { mutableStateOf("100") }
    var saving by remember { mutableStateOf(false) }
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
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // H5 CategoriesView.vue .tabs：白卡片内双格，选中浅青底，无整条灰轨道
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("expense" to "支出", "income" to "收入").forEach { (v, label) ->
                val sel = tab == v
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .segmentToggleClickable { tab = v }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (sel) PrimaryDark else Muted,
                    )
                }
            }
        }

        // H5 .add.card：横向输入 + 主色「添加」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            H5CompactInputField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "新分类名称",
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
            ) { Text("添加", fontWeight = FontWeight.ExtraBold) }
        }

        if (loading) {
            Text("加载中…", color = Muted, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
        } else if (rows.isEmpty()) {
            Text(
                "暂无分类",
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
                rows.forEachIndexed { index, c ->
                    val id = c.get("id").asLong
                    val name = c.get("name")?.asString ?: ""
                    val sort = c.get("sort_order")?.asInt ?: 0
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (editId == id) {
                            H5CompactInputField(
                                value = editName,
                                onValueChange = { editName = it },
                                placeholder = "分类名称",
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
                                                    AppServices.ledgerRepository.updateCategory(id, editName.trim(), so)
                                                }
                                                editId = null
                                                onSuccess("已保存")
                                                val arr = withContext(Dispatchers.IO) {
                                                    AppServices.ledgerRepository.listCategories(tab).mapJsonObjects()
                                                }
                                                rows = arr
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
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text(
                                        "排序 $sort",
                                        fontSize = 11.sp,
                                        color = Muted,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = {
                                            editId = id
                                            editName = name
                                            editSort = sort.toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Muted, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
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
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "删除",
                                            tint = Expense.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (index < rows.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = Line.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
