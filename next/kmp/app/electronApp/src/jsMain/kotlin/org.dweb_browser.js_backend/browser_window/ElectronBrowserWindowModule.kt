package org.dweb_browser.js_backend.browser_window

import org.dweb_browser.js_backend.view_model.BaseViewModel
import org.dweb_browser.js_backend.view_model_state.ViewModelMutableMap
import org.dweb_browser.js_backend.view_model.DecodeValueFromString
import org.dweb_browser.js_backend.view_model.EncodeValueToString


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
    val viewModel: BaseViewModel
}

class ElectronBrowserWindowModule(
    override val subDomain: String, /* example: demo.compose.app */
    val encodeValueToString: EncodeValueToString,
    val decodeValueFromString: DecodeValueFromString,
    initVieModelMutableMap: ViewModelMutableMap
) : IElectronBrowserWindowModule{
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController.create(subDomain)
    override val viewModel: BaseViewModel = BaseViewModel(
        subDomain = subDomain,
        encodeValueToString = encodeValueToString,
        decodeValueFromString = decodeValueFromString,
        initVieModelMutableMap = initVieModelMutableMap
    )
    init {
        viewModel.onUpdateByClient{key: String, value: dynamic ->
            console.error("server received data from client key: value", key, ":", value)

//            viewModel[key] = value + 1
        }
        controller.open(ElectronBrowserWindowController.createBrowserWindowOptions().apply {
            width = 1300.0
            height = 1000.0
        })
    }
}


