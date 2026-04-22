package com.myledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.myledger.app.ui.navigation.AppRoot
import com.myledger.app.ui.theme.MyLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyLedgerTheme {
                AppRoot()
            }
        }
    }
}
