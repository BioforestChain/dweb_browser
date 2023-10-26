package org.dweb_browser.shared.microService.sys.toast

expect object ToastController {

    fun show(
        text: String,
        durationType: DurationType,
        positionType: PositionType
    )
}