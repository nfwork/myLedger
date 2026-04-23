package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.AppServices
import com.myledger.app.data.remote.ApiException
import com.myledger.app.ui.theme.Bg
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.TealLight
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.h5Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onError: (String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                        Brush.linearGradient(listOf(TealLight, PrimaryDark)),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("◇", color = Color(0xFFECFDF5), fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(12.dp))
            Text("myLedger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("简单记账，清晰收支", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("用户名", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            H5CompactInputField(
                value = username,
                onValueChange = { username = it.trimStart() },
                placeholder = "至少 4 个字符",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("密码", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            H5CompactInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "至少 6 位",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            err?.let {
                Text(it, color = Expense, fontSize = 14.sp)
            }
            Button(
                onClick = {
                    err = null
                    loading = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AppServices.authRepository.login(username.trim(), password)
                            }
                            onLoggedIn()
                        } catch (e: ApiException) {
                            err = e.message
                            onError(e.message ?: "登录失败")
                        } catch (e: HttpException) {
                            val msg = when (e.code()) {
                                401 -> "账号或密码不对，请核对后再试；新用户可先注册"
                                else -> "暂时无法登录，请稍后再试"
                            }
                            err = msg
                            onError(msg)
                        } catch (_: Exception) {
                            val msg = "当前连不上服务器，请检查网络或确认服务已开启"
                            err = msg
                            onError(msg)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && username.length >= 4 && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(if (loading) "登录中…" else "登录")
            }
            Text(
                "还没有账号？去注册",
                color = Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clickable { onRegister() },
            )
        }
    }
}
