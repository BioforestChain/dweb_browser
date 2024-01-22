package org.dweb_browser.js_backend.http

sealed class MatchPattern(key: String){
    class Full(): MatchPattern("FULL")
    class Prefix(): MatchPattern("PREFIX")

    companion object{
        val FULL = Full()
        val PREFIX = Prefix()
    }
}