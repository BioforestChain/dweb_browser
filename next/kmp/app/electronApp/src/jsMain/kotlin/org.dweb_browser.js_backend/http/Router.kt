package org.dweb_browser.js_backend.http

class Router(){
    private val _routes = mutableMapOf<String, Route>()

    fun getAllRoutes() = _routes
    fun add(route: Route){
        _routes[route.path] = route
    }

    fun remove(path: String) = _routes.remove(path)
    fun remove(route: Route) = _routes.remove(route.path)
}