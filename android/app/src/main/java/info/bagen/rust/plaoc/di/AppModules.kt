package info.bagen.rust.plaoc.di

import info.bagen.rust.plaoc.microService.webview.DWebBrowserModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    viewModel { DWebBrowserModel() }
}