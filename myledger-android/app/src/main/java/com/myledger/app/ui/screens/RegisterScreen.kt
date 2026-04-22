package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.AppServices
import com.myledger.app.data.remote.ApiException
import com.myledger.app.ui.theme.Bg
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TealLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onDone: () -> Unit,
    onLogin: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 28.dp)) {
            Box(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF5EEAD4), Primary)),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color(0xFF042F2E), fontSize = 32.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(12.dp))
            Text("创建账号", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "注册后自动创建默认资金账户与常用分类",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(Surface, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.trimStart() },
                label = { Text("用户名") },
                placeholder = { Text("至少 4 个字符") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                placeholder = { Text("至少 6 位") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password2,
                onValueChange = { password2 = it },
                label = { Text("确认密码") },
                placeholder = { Text("请再次输入密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.trimStart() },
                label = { Text("昵称（可选）") },
                placeholder = { Text("显示名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            err?.let { Text(it, color = Expense, fontSize = 14.sp) }
            Button(
                onClick = {
                    err = null
                    if (password != password2) {
                        err = "两次输入的密码不一致"
                        onError(err!!)
                        return@Button
                    }
                    loading = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.register(
                                    username.trim(),
                                    password,
                                    nickname.trim().ifBlank { null },
                                )
                            }
                            onSuccess("注册成功，请登录")
                            onDone()
                        } catch (e: ApiException) {
                            err = e.message
                            onError(e.message ?: "注册失败")
                        } catch (e: Exception) {
                            err = e.message ?: "注册失败"
                            onError(err!!)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && username.length >= 4 && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(if (loading) "提交中…" else "注册")
            }
            Text(
                "已有账号？去登录",
                color = Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clickable { onLogin() },
            )
        }
    }
}
