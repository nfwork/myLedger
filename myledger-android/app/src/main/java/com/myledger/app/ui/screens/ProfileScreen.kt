package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.Line
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.h5Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    user: JsonObject?,
    onUserChange: (JsonObject?) -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onPassword: () -> Unit,
    onLogout: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user) {
        nickname = user?.get("nickname")?.asString ?: ""
    }

    LaunchedEffect(Unit) {
        val me = withContext(Dispatchers.IO) { AppServices.authRepository.fetchMe() }
        onUserChange(me)
        nickname = me?.get("nickname")?.asString ?: ""
    }

    val username = user?.get("username")?.asString ?: "—"
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0F766E)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(username, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(
                    nickname.ifBlank { "未设置昵称" },
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(16.dp),
        ) {
            Text("昵称", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            H5CompactInputField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "显示名称",
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.updateProfile(nickname.trim())
                            }
                            val me = withContext(Dispatchers.IO) { AppServices.authRepository.fetchMe() }
                            onUserChange(me)
                            onSuccess("昵称已更新")
                        } catch (e: Exception) {
                            onError(e.message ?: "保存失败")
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(if (saving) "保存中…" else "保存昵称")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card(),
        ) {
            listOf(
                "资金账户" to onAccounts,
                "分类管理" to onCategories,
                "修改密码" to onPassword,
            ).forEachIndexed { i, t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = t.second)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                if (i < 2) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line),
                    )
                }
            }
        }

        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDark),
        ) {
            Text("退出登录")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { AppServices.authRepository.logout() }
                            } catch (_: Exception) { }
                            onUserChange(null)
                            onLogout()
                        }
                    }
                ) { Text("退出", color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}
