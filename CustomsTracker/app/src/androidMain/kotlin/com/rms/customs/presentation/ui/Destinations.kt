package com.rms.customs.presentation.ui

object Dest {
    const val LOADING            = "loading"
    const val LOGIN              = "login"
    const val SETUP              = "setup"
    const val MAIN               = "main"
    const val TRANSACTION_DETAIL = "transaction/{id}"
    const val CREATE             = "create_transaction"
    const val NOTIFICATIONS      = "notifications"
    const val SLA_ADMIN          = "sla_admin"
    const val REPORTS            = "reports"
    const val SETTINGS           = "settings"
    const val USER_MANAGEMENT    = "user_management"

    fun transactionDetail(id: String) = "transaction/$id"
}
