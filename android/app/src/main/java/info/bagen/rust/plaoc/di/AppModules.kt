package info.bagen.rust.plaoc.di

import info.bagen.rust.plaoc.microService.browser.DWebBrowserModel
import info.bagen.rust.plaoc.ui.app.AppRepository
import info.bagen.rust.plaoc.ui.app.AppViewModel
import info.bagen.rust.plaoc.ui.camera.QRCodeViewModel
import info.bagen.rust.plaoc.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    viewModel { DWebBrowserModel() }
    viewModel { MainViewModel() }
    viewModel { AppViewModel(get()) }
    viewModel { QRCodeViewModel() }
}

val repositoryModule = module {
    single { AppRepository() }
}