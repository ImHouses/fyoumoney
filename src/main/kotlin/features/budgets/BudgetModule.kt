package dev.jcasas.features.budgets

import org.koin.dsl.module

val budgetModule =
    module {
        single { BudgetRepository() }
        single { BudgetService(get(), get()) }
    }
