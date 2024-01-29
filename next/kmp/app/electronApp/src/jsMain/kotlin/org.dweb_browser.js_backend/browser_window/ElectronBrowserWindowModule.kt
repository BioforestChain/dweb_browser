package org.dweb_browser.js_backend.browser_window

import electron.BrowserWindowConstructorOptions
import org.dweb_browser.js_backend.http.HttpServer
import org.dweb_browser.js_backend.view_model.BaseViewModel
import org.dweb_browser.js_backend.view_model_state.ViewModelMutableMap
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowController


/**
 * 定义一个专门用在Electron平台中的Module
 *
 * 1. 功能目标
 * - 以Electron平台为基础，
 * - 可以独立运行的程序
 *
 *
 * 2. 设计概述
 * - moduleId: 标识符
 * - controller： window控制容器
 * - viewModel: 页面内容数据
 */
interface IElectronBrowserWindowModule{
    val moduleId: String
    val controller: ElectronBrowserWindowController
    val viewModel: BaseViewModel
}

class ElectronBrowserWindowModule(
    override val moduleId: String, /* example: demo.compose.app */
    initVieModelMutableMap: ViewModelMutableMap? = null
) : IElectronBrowserWindowModule{
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController.create(moduleId)
    override val viewModel: BaseViewModel = BaseViewModel(moduleId, initVieModelMutableMap)
    init {
        viewModel.onUpdateByClient{key: dynamic, value: dynamic ->
            viewModel[key] = value + 1
        }
        controller.open(ElectronBrowserWindowController.createBrowserWindowOptions().apply {
            width = 1300.0
            height = 1000.0
        })

    }
}


