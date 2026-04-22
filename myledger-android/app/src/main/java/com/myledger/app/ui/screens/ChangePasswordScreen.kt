package com.myledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myledger.app.AppServices
import com.myledger.app.ui.theme.Expense
import com.myledger.app.ui.theme.Primary
import com.myledger.app.ui.theme.Surface
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
            .padding(bottom = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("当前密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = newPassword2,
                onValueChange = { newPassword2 = it },
                label = { Text("确认新密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            err?.let { Text(it, color = Expense, modifier = Modifier.padding(bottom = 8.dp)) }
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
