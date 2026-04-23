package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.AppServices
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.H5CompactInputField
import com.myledger.app.ui.theme.Muted
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.ScreenPadding
import com.myledger.app.ui.theme.Surface
import com.myledger.app.ui.theme.h5Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPassword2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .h5Card()
                .padding(16.dp),
        ) {
            Text("当前密码", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            H5CompactInputField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = "请输入当前密码",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Text("新密码", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            H5CompactInputField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = "至少 6 位",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Text("确认新密码", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            H5CompactInputField(
                value = newPassword2,
                onValueChange = { newPassword2 = it },
                placeholder = "请再次输入新密码",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            err?.let { Text(it, color = Expense, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            Button(
                onClick = {
                    err = null
                    if (newPassword != newPassword2) {
                        err = "两次输入的新密码不一致"
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AppServices.ledgerRepository.changePassword(oldPassword, newPassword)
                            }
                            onSuccess("密码已更新")
                            onDone()
                        } catch (e: Exception) {
                            err = e.message ?: "修改失败"
                            onError(err!!)
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving && oldPassword.isNotBlank() && newPassword.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(if (saving) "提交中…" else "确认修改")
            }
        }
    }
}
