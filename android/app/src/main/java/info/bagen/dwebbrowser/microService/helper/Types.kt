package info.bagen.dwebbrowser.microService.helper


typealias Mmid = String;

enum class EIpcEvent(val event:String){
    State("state"),
    Ready("ready"),
    Activity("activity"),
    Close("close")
}

typealias DWEB_DEEPLINK = String;