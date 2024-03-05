package org.dweb_browser.js_common.state_compose.role

import kotlinx.coroutines.flow.MutableSharedFlow

interface IRoleFlow {
    val roleFlow: MutableSharedFlow<Role>
}