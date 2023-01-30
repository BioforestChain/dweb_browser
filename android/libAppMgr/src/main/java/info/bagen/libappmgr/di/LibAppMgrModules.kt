package info.bagen.libappmgr.di

import info.bagen.libappmgr.ui.app.AppRepository
import info.bagen.libappmgr.ui.app.AppViewModel
import info.bagen.libappmgr.ui.dcim.DCIMViewModel
import info.bagen.libappmgr.ui.download.DownLoadRepository
import info.bagen.libappmgr.ui.download.DownLoadViewModel
import info.bagen.libappmgr.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val libViewModelModule = module {
    viewModel { MainViewModel() }
    viewModel { AppViewModel(get()) }
    viewModel { DownLoadViewModel(get()) }
    viewModel { DCIMViewModel(get()) }
}

val libRepositoryModule = module {
    single { AppRepository() }
    single { DownLoadRepository() }
}
