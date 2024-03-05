package org.dweb_browser.js_common.state_compose.role

sealed class Role{
    class Client (@JsName("value") val value: String = "CLIENT"): Role()
    class Server (@JsName("value") val value: String = "SERVER"): Role()
    companion object{
        val CLIENT = Client()
        val SERVER = Server()
    }
}