package com.myledger.app

import android.app.Application

class MyLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServices.init(this)
    }
}
