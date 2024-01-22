package org.dweb_browser.js_backend.http

sealed class Method(val value: String){
    class Get(): Method("GET")
    class Post(): Method("POST")

    companion object{
        val GET = Get()
        val POST = Post()
    }
}