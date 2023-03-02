package info.bagen.rust.plaoc.di

import info.bagen.rust.plaoc.microService.browser.DWebBrowserModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    viewModel { DWebBrowserModel() }
}