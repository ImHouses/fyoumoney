package dev.jcasas.features.transactions

import org.koin.dsl.module

val transactionModule = module {
    single { TransactionRepository() }
    single { TransactionService(get(), get(), get()) }
}
