package com.myledger.app

import android.content.Context
import com.myledger.app.data.remote.ApiClient
import com.myledger.app.data.repo.AuthRepository
import com.myledger.app.data.repo.LedgerRepository
import com.myledger.app.data.store.AccountScopeStore
import com.myledger.app.data.store.TokenStore

object AppServices {
    lateinit var tokenStore: TokenStore
        private set
    lateinit var accountScopeStore: AccountScopeStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var ledgerRepository: LedgerRepository
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        tokenStore = TokenStore(app)
        accountScopeStore = AccountScopeStore(app)
        val client = ApiClient.create(app, tokenStore)
        authRepository = AuthRepository(client, tokenStore)
        ledgerRepository = LedgerRepository(client)
    }
}
