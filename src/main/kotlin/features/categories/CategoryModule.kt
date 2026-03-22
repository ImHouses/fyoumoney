package dev.jcasas.features.categories

import org.koin.dsl.module

val categoryModule =
    module {
        single { CategoryRepository() }
        single { CategoryService(get()) }
    }
