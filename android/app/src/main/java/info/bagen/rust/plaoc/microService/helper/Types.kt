package info.bagen.rust.plaoc.microService.helper


typealias Mmid = String;

enum class EIpcEvent(val event:String){
    State("state"),
    Ready("ready"),
    Activity("activity")
}
