package org.dweb_browser.js_backend.view_model

import org.dweb_browser.js_common.view_model.CommonViewModelState
import org.dweb_browser.js_common.view_model.SyncType
import org.dweb_browser.js_common.view_model.ViewModelMutableMap
import org.dweb_browser.js_common.view_model.ViewModelStateRole

class ViewModelState(
    initState: ViewModelMutableMap
): CommonViewModelState(initState){
    /**
     * - 为服务器角色添加一个更新状态的快捷方式
     * - viewModelState[key] = value
     */
    operator fun set(key: String, value:dynamic){
        set(key, value, ViewModelStateRole.SERVER, SyncType.REPLACE)
    }
}



