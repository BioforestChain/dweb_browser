package org.dweb_browser.js_backend.http

class Router(){
    private val _routes = mutableMapOf<String, Route>()

    fun getAllRoutes() = _routes
    fun add(route: Route){
        console.log("添加了route", route.subDomain)
        _routes["${route.subDomain}-${route.path}"] = route
    }

    fun remove(key: String) = _routes.remove(key)
    fun remove(route: Route) = _routes.remove("${route.subDomain}-${route.path}")
}