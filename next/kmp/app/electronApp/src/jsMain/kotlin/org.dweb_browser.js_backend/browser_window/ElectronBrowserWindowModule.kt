package org.dweb_browser.js_backend.browser_window

import org.dweb_browser.js_backend.view_model.ViewModel
import org.dweb_browser.js_common.view_model.DataState


/**
 * 定义一个专门用在Electron平台中的Module
 *
 * 1. 功能目标
 * - 以Electron平台为基础，
 * - 可以独立运行的程序
 *
 *
 * 2. 设计概述
 * - subDomain: 标识符
 * - controller： window控制容器
 * - viewModel: 页面内容数据
 */
interface IElectronBrowserWindowModule{
    val subDomain: String
    val controller: ElectronBrowserWindowController
    val viewModel: ViewModel
}

class ElectronBrowserWindowModule(
    override val subDomain: String, /* example: demo.compose.app */
    dataState: DataState
) : IElectronBrowserWindowModule{
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController.create(subDomain)
    override val viewModel: ViewModel = ViewModel(
        subDomain = subDomain,
        dataState = dataState
    )
}


